package com.gradion.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Stores generated images on the local filesystem under {@code upload.dir}.
 * Images are served back through {@code /api/files/...} — no S3, no blob store,
 * per the spec's "images live on the local filesystem" requirement.
 */
@Slf4j
@Service
public class ImageStorageService {

    private final Path root;

    public ImageStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory: " + root, e);
        }
    }

    /**
     * Saves image bytes to {@code uploadDir/{projectId}/{date}/{contentHash}.png}
     * and returns a path relative to the upload root, safe to expose to the UI.
     *
     * @return relative path like {@code 12/2026-08-13/a1b2c3.png}
     */
    public String saveImage(Long projectId, byte[] imageData, String mimeType) {
        String ext = switch (mimeType == null ? "" : mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "png";
        };
        String hash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(imageData);
            hash = HexFormat.of().formatHex(bytes).substring(0, 32);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash image data", e);
        }

        String datePrefix = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Path dir = root.resolve(String.valueOf(projectId)).resolve(datePrefix);
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(hash + "." + ext);
            if (!Files.exists(file)) {
                Files.write(file, imageData);
            }
            String relative = root.relativize(file).toString().replace('\\', '/');
            log.info("Saved image {} ({} bytes)", relative, imageData.length);
            return relative;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write image for project " + projectId, e);
        }
    }

    /**
     * Resolves a stored relative path back to a readable file, guarding against
     * path traversal (only allow files under the upload root).
     */
    public Path resolveStored(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid image path: " + relativePath);
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("Image not found: " + relativePath);
        }
        return resolved;
    }
}