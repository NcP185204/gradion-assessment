# 09 - Gemini REST Client: File Upload, Interactions & Image Generation

## 1. User Prompt
Implement a plain-REST Gemini client (no Google SDK) in
com.gradion.backend.service.gemini.GeminiClient.java.

Requirements:
- File API: upload the book text once, get back a files/... URI.
- Interactions API: create interactions and chain them via previous_interaction_id.
- Structured output via response_format with a JSON schema (characters/chapters).
- Image generation on the Nano Banana image model; decode base64 image data.
- Send the API key via the x-goog-api-key header.

---

## 2. Gemini Response (Summary)
- **File upload** (`uploadBookText`): a multipart request with JSON metadata
  plus the book bytes, then poll until the file is `ACTIVE` and return its URI.
- **Interactions** (`createInteraction` / `createTextInteraction` /
  `createImageInteraction`): POST to `/v1beta/interactions` with `model`,
  `input`, and `previous_interaction_id` for chaining; structured output is
  requested with a `response_format` JSON schema.
- **Parsing** (`parseInteraction`): walks the interaction `steps` looking for
  `model_output` parts — text becomes `outputText`, base64 image parts become
  decoded `GeminiImage` records.
- **Schema helpers** (`promptArraySchema`) for the `[{name, prompt}]` shape the
  notebook uses for characters and chapters.
- Two `RestClient`s (JSON base and upload base), both adding the
  `x-goog-api-key` header.

---

## 3. Developer Notes / Actions Taken
- I kept the two `RestClient` builders separate (JSON vs upload base URL) so the
  key header is added exactly once per client and both endpoints get it.
- The first build failed to link because `MultipartBodyBuilder` references
  reactor / reactive-streams types that `spring-boot-starter-web` does not pull
  in. I added the `reactor-core` dependency (version managed by the Boot BOM)
  to fix the book-upload path.
- During the end-to-end run I tested the endpoints directly with curl to prove
  the key itself was fine, which showed the 403/401 errors came from the key
  not being loaded, not from this client.

