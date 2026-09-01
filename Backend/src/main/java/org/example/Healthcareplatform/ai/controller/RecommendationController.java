package org.example.Healthcareplatform.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.dto.RecommendationRequest;
import org.example.Healthcareplatform.ai.dto.RecommendationResponse;
import org.example.Healthcareplatform.ai.exception.AIException;
import org.example.Healthcareplatform.ai.service.RecommendationService;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final SecurityContextUtil securityContextUtil;

    @PostMapping
    public ResponseEntity<RecommendationResponse> getRecommendations(@RequestBody RecommendationRequest request) {
        Long userId = securityContextUtil.getCurrentUserId();
        log.info("POST /api/recommendations — userId={}, query={}, allergies={}",
                userId, request.getQuery(), request.getAllergies());

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        RecommendationResponse response = recommendationService.getRecommendations(request, userId);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(AIException.class)
    public ResponseEntity<Map<String, String>> handleAIException(AIException ex) {
        log.error("Recommendation AI error: {}", ex.getMessage(), ex);
        String detail = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "error", ex.getMessage(),
                        "detail", detail
                ));
    }
}
