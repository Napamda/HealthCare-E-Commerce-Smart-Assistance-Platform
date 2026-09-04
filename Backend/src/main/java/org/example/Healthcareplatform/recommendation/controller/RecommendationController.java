package org.example.Healthcareplatform.recommendation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getRecommendations(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        log.info("GET /api/recommendations — userId={}", userId);
        // Return popular products as recommendations
        return ResponseEntity.ok(productService.getPopularProducts(10));
    }

    @GetMapping("/personalized")
    public ResponseEntity<List<Product>> getPersonalizedRecommendations(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        log.info("GET /api/recommendations/personalized — userId={}", userId);
        // Return personalized recommendations based on user's order history
        return ResponseEntity.ok(productService.getPersonalizedRecommendations(userId, 8));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getRecommendationsByCategory(
            @PathVariable String category,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        log.info("GET /api/recommendations/category/{} — userId={}", category, userId);
        return ResponseEntity.ok(productService.getRecommendationsByCategory(category, 6));
    }
}