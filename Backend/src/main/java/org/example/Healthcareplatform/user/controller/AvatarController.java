package org.example.Healthcareplatform.user.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

/**
 * Public avatar serving endpoint. Filenames are server-generated UUIDs
 * (unguessable) and strictly validated, so unauthenticated GET is safe.
 * This keeps <img src> simple — image tags can't attach Authorization
 * headers.
 */
@RestController
@Slf4j
public class AvatarController {

    private static final Map<String, String> TYPE_MAP = Map.of(
            "jpg", MediaType.IMAGE_JPEG_VALUE,
            "jpeg", MediaType.IMAGE_JPEG_VALUE,
            "png", MediaType.IMAGE_PNG_VALUE,
            "webp", "image/webp",
            "gif", MediaType.IMAGE_GIF_VALUE
    );

    @Value("${user.avatar.storage-root}")
    private String avatarStorageRoot;

    @GetMapping("/api/avatars/{filename}")
    public ResponseEntity<FileSystemResource> serveAvatar(@PathVariable String filename) {
        // Strict allow-list: UUID.ext only — no path traversal possible.
        if (!filename.matches("[0-9a-fA-F\\-]{36}\\.(jpg|jpeg|png|webp|gif)")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Path path = Paths.get(avatarStorageRoot).resolve(filename);
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String ext = filename.substring(filename.lastIndexOf('.') + 1);
        MediaType type = MediaType.parseMediaType(TYPE_MAP.getOrDefault(ext, "application/octet-stream"));

        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .body(new FileSystemResource(path));
    }
}
