package com.gradion.backend.service.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Plain-REST client for the Gemini API.
 *
 * <p>Mirrors the pipeline from Google's "Book illustration" notebook
 * (https://colab.research.google.com/github/google-gemini/cookbook/blob/main/examples/Book_illustration.ipynb)
 * without the Google SDK: every call is a documented HTTP endpoint.
 *
 * <ul>
 *   <li>File API — upload the book text once, get back a {@code files/...} URI.</li>
 *   <li>Interactions API — {@code POST /v1beta/interactions}; interactions are chained via
 *       {@code previous_interaction_id} so the book is never re-sent.</li>
 *   <li>Structured output — {@code response_format} with a JSON schema.</li>
 *   <li>Image generation — same interactions endpoint on the Nano Banana image model;
 *       images come back base64-encoded inside the interaction {@code steps}.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiClient {

    public record GeminiImage(String mimeType, byte[] data) {}

    public record GeminiInteraction(String id, String outputText, List<GeminiImage> images) {}

    private static final String JSON_BASE = "https://generativelanguage.googleapis.com/v1beta";
    private static final String UPLOAD_BASE = "https://generativelanguage.googleapis.com/upload/v1beta";

    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.text-model}")
    private String textModel;

    @Value("${app.gemini.image-model}")
    private String imageModel;

    // ------------------------------------------------------------------
    // File API — upload book text once, reuse the URI everywhere.
    // ------------------------------------------------------------------

    /**
     * Uploads the book text and waits for the file to become ACTIVE.
     *
     * @return the file URI (e.g. https://generativelanguage.googleapis.com/v1beta/files/abc)
     */
    public String uploadBookText(String displayName, String bookText) {
        // Step 1: start a resumable upload and get the file name.
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        ObjectNode metadata = objectMapper.createObjectNode();
        ObjectNode fileMeta = objectMapper.createObjectNode();
        fileMeta.put("display_name", displayName);
        metadata.set("file", fileMeta);

        builder.part("metadata", metadata.toString(), MediaType.APPLICATION_JSON);
        builder.part("file", new ByteArrayResource(bookText.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return displayName;
            }
        }, MediaType.parseMediaType("text/plain"));

        JsonNode uploadResp = uploadClient().post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(JsonNode.class);

        String fileUri = uploadResp.path("file").path("uri").asText(null);
        String fileName = uploadResp.path("file").path("name").asText(null);
        if (fileUri == null || fileUri.isBlank()) {
            throw new RestClientException("Gemini file upload did not return a file URI: " + uploadResp);
        }

        // Step 2: wait for the file to be ACTIVE (text files are fast, but be safe).
        String state = uploadResp.path("file").path("state").asText("PROCESSING");
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis();
        while (!"ACTIVE".equals(state) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            JsonNode check = jsonClient().get()
                    .uri("/files/{fileName}", fileName)
                    .retrieve()
                    .body(JsonNode.class);
            state = check.path("file").path("state").asText("UNKNOWN");
        }
        if (!"ACTIVE".equals(state)) {
            throw new RestClientException("Gemini file never became ACTIVE (state=" + state + ")");
        }
        log.info("Uploaded book file {} -> {}", displayName, fileUri);
        return fileUri;
    }

    // ------------------------------------------------------------------
    // Interactions API — the conversation/pipeline primitive.
    // ------------------------------------------------------------------

    /**
     * Creates an interaction on the configured text model.
     *
     * @param prompt              plain-text message
     * @param previousInteraction chained interaction id, or {@code null}
     * @param jsonSchema          response_format schema, or {@code null}
     */
    public GeminiInteraction createTextInteraction(String prompt, String previousInteraction,
                                                   JsonNode jsonSchema) {
        return createInteraction(textModel, prompt, previousInteraction, jsonSchema);
    }

    /**
     * Creates a multi-part interaction (text + document URI reference).
     * Used once per project to give Gemini the book without re-sending it.
     */
    public GeminiInteraction createBookInteraction(String fileUri) {
        ArrayNode input = objectMapper.createArrayNode();
        ObjectNode text = objectMapper.createObjectNode();
        text.put("type", "text");
        text.put("text", "Here's a book, to illustrate using Nano Banana. Don't say anything for now, instructions will follow.");
        ObjectNode doc = objectMapper.createObjectNode();
        doc.put("type", "document");
        doc.put("uri", fileUri);
        input.add(text);
        input.add(doc);
        return createInteraction(textModel, input, null, null);
    }

    /**
     * Creates an interaction on the configured image model (Nano Banana family).
     */
    public GeminiInteraction createImageInteraction(String prompt, String previousInteraction) {
        return createInteraction(imageModel, prompt, previousInteraction, null);
    }

    private GeminiInteraction createInteraction(String model, Object input, String previousInteraction,
                                                JsonNode jsonSchema) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.set("input", input instanceof String s ? objectMapper.getNodeFactory().textNode(s) : (JsonNode) input);
        if (previousInteraction != null && !previousInteraction.isBlank()) {
            body.put("previous_interaction_id", previousInteraction);
        }
        if (jsonSchema != null) {
            ObjectNode responseFormat = objectMapper.createObjectNode();
            responseFormat.put("type", "text");
            responseFormat.put("mime_type", "application/json");
            responseFormat.set("schema", jsonSchema);
            body.set("response_format", responseFormat);
        }

        try {
            JsonNode resp = jsonClient().post()
                    .uri("/interactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return parseInteraction(resp);
        } catch (RestClientException e) {
            log.error("Gemini interaction failed for model={} previous={}", model, previousInteraction, e);
            throw e;
        }
    }

    private GeminiInteraction parseInteraction(JsonNode resp) {
        String id = resp.path("id").asText(null);
        String outputText = resp.path("output_text").asText(null);

        List<GeminiImage> images = new ArrayList<>();
        JsonNode steps = resp.path("steps");
        for (JsonNode step : steps) {
            if (!"model_output".equals(step.path("type").asText())) {
                continue;
            }
            JsonNode content = step.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode part : content) {
                if ("image".equals(part.path("type").asText())) {
                    String data = part.path("data").asText(null);
                    String mimeType = part.path("mime_type").asText("image/png");
                    if (data != null) {
                        images.add(new GeminiImage(mimeType, Base64.getDecoder().decode(data)));
                    }
                } else if (outputText == null && "text".equals(part.path("type").asText())) {
                    outputText = part.path("text").asText(null);
                }
            }
        }
        return new GeminiInteraction(id, outputText, images);
    }

    // ------------------------------------------------------------------
    // Schema helpers for structured output ("only the adults", max 2).
    // ------------------------------------------------------------------

    /**
     * JSON schema for {@code [{"name": string, "prompt": string}]} — the
     * {@code Prompt} Pydantic model from the notebook.
     */
    public JsonNode promptArraySchema() {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "object");

        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode name = objectMapper.createObjectNode();
        name.put("type", "string");
        name.put("title", "Name");
        ObjectNode prompt = objectMapper.createObjectNode();
        prompt.put("type", "string");
        prompt.put("title", "Prompt");
        props.set("name", name);
        props.set("prompt", prompt);
        item.set("properties", props);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("name");
        required.add("prompt");
        item.set("required", required);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "array");
        schema.set("items", item);
        return schema;
    }

    // ------------------------------------------------------------------

    private RestClient jsonClient() {
        return restClientBuilder
                .baseUrl(JSON_BASE)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    private RestClient uploadClient() {
        return restClientBuilder
                .baseUrl(UPLOAD_BASE)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }
}