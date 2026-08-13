package com.gradion.backend.controller;

import com.gradion.backend.service.ImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

/**
 * Serves generated images from the local filesystem.
 *
 * <p>The stored relative path (e.g. {@code 12/2026-08-13/a1b2c3.png}) is
 * exposed by the project detail DTO; the frontend fetches
 * {@code /api/files/{projectId}/{date}/{hash}.png} through the authenticated
 * client so images are never publicly hosted.
 */
@RestController
@RequestMapping("/api/files/{projectId}")
@RequiredArgsConstructor
public class FileController {

    private final ImageStorageService imageStorageService;

    @GetMapping("/**")
    public ResponseEntity<FileSystemResource> serveFile(
            @PathVariable Long projectId,
            HttpServletRequest request) {

        String uri = request.getRequestURI();
        String marker = "/api/files/" + projectId + "/";
        String relativePath;
        if (uri.startsWith(marker)) {
            relativePath = uri.substring(marker.length());
        } else {
            String prefix = "/api/files/";
            relativePath = uri.substring(uri.indexOf(prefix) + prefix.length());
        }

        if (relativePath == null || relativePath.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path file = imageStorageService.resolveStored(relativePath);
            int dot = relativePath.lastIndexOf('.');
            String ext = dot >= 0 ? relativePath.substring(dot + 1).toLowerCase() : "png";
            MediaType mediaType = switch (ext) {
                case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                case "webp" -> MediaType.parseMediaType("image/webp");
                default -> MediaType.IMAGE_PNG;
            };
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(new FileSystemResource(file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}