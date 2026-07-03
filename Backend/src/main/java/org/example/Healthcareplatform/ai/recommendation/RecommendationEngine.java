package org.example.Healthcareplatform.ai.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.dto.RecommendationRequest;
import org.example.Healthcareplatform.ai.dto.RecommendationResponse;
import org.example.Healthcareplatform.ai.provider.AIProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final AIProvider aiProvider;
    private final ObjectMapper objectMapper;

    public RecommendationResponse generateRecommendations(RecommendationRequest request) {
        String prompt = buildRecommendationPrompt(request);
        log.info("RecommendationEngine — generating recommendations for query: {}", request.getQuery());

        String aiResponse = aiProvider.chat(prompt);
        return parseAiResponse(aiResponse, request);
    }

    private String buildRecommendationPrompt(RecommendationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a healthcare product recommendation assistant. ");
        sb.append("Based on the user's needs, recommend suitable healthcare products and provide health tips.\n\n");

        sb.append("User context:\n");
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            sb.append("- Query: ").append(request.getQuery()).append("\n");
        }
        if (request.getSymptom() != null && !request.getSymptom().isBlank()) {
            sb.append("- Symptom: ").append(request.getSymptom()).append("\n");
        }
        if (request.getCondition() != null && !request.getCondition().isBlank()) {
            sb.append("- Health condition: ").append(request.getCondition()).append("\n");
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            sb.append("- Preferred category: ").append(request.getCategory()).append("\n");
        }
        if (request.getAllergies() != null && !request.getAllergies().isEmpty()) {
            sb.append("- Known allergies: ").append(String.join(", ", request.getAllergies())).append("\n");
        }
        if (request.getRecentProductCategories() != null && !request.getRecentProductCategories().isEmpty()) {
            sb.append("- Recently browsed categories: ").append(String.join(", ", request.getRecentProductCategories())).append("\n");
        }

        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 5;
        sb.append("\nPlease respond in the following JSON format only, no other text:\n");
        sb.append("{\n");
        sb.append("  \"reasoning\": \"brief explanation of recommendations\",\n");
        sb.append("  \"products\": [\n");
        sb.append("    {\"productName\": \"...\", \"category\": \"...\", \"description\": \"...\", \"reason\": \"...\", \"confidenceScore\": 0.0}\n");
        sb.append("  ],\n");
        sb.append("  \"healthTips\": [\n");
        sb.append("    {\"title\": \"...\", \"content\": \"...\", \"category\": \"...\"}\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("\nProvide exactly ").append(maxResults).append(" product recommendations. ");
        sb.append("Include 1-3 relevant health tips. ");
        sb.append("Ensure product descriptions mention key ingredients for allergy checking. ");
        sb.append("Set confidenceScore between 0.0 and 1.0.");

        return sb.toString();
    }

    private RecommendationResponse parseAiResponse(String aiResponse, RecommendationRequest request) {
        try {
            String json = extractJson(aiResponse);
            RecommendationResponse response = objectMapper.readValue(json, RecommendationResponse.class);

            if (response.getProducts() == null) {
                response.setProducts(new ArrayList<>());
            }
            if (response.getHealthTips() == null) {
                response.setHealthTips(new ArrayList<>());
            }

            return response;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse AI recommendation response, returning raw text", e);
            return RecommendationResponse.builder()
                    .reasoning(aiResponse)
                    .products(new ArrayList<>())
                    .healthTips(new ArrayList<>())
                    .build();
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
