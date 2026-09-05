package org.example.Healthcareplatform.product.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.product.dto.ImageResponse;
import org.example.Healthcareplatform.product.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/api/products/{productId}/images")
    public ResponseEntity<List<ImageResponse>> getProductImages(@PathVariable Long productId) {
        log.info("GET /api/products/{}/images", productId);
        return ResponseEntity.ok(imageService.getImages(productId));
    }

    @PostMapping("/api/admin/products/{productId}/images")
    public ResponseEntity<ImageResponse> uploadProductImage(@PathVariable Long productId,
                                                            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/admin/products/{}/images — file={}", productId, file.getOriginalFilename());
        return ResponseEntity.ok(imageService.uploadImage(productId, file));
    }

    @PutMapping("/api/admin/products/images/{imageId}/primary")
    public ResponseEntity<ImageResponse> setPrimary(@PathVariable Long imageId) {
        return ResponseEntity.ok(imageService.setPrimary(imageId));
    }

    @DeleteMapping("/api/admin/products/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        imageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }
}
