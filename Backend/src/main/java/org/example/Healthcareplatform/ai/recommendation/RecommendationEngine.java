package org.example.Healthcareplatform.ai.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.dto.RecommendationRequest;
import org.example.Healthcareplatform.ai.dto.RecommendationResponse;
import org.example.Healthcareplatform.ai.provider.AIProvider;
import org.example.Healthcareplatform.product.dto.ProductRef;
import org.example.Healthcareplatform.product.service.ProductService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RecommendationEngine {

    private final AIProvider aiProvider;
    private final ObjectMapper objectMapper;
    private final ProductService productService;

    public RecommendationEngine(AIProvider aiProvider, ObjectMapper objectMapper, ProductService productService) {
        this.aiProvider = aiProvider;
        this.objectMapper = objectMapper;
        this.productService = productService;
    }

    public RecommendationResponse generateRecommendations(RecommendationRequest request) {
        String prompt = buildRecommendationPrompt(request);
        log.info("RecommendationEngine — generating recommendations for query: {}", request.getQuery());

        String aiResponse = aiProvider.chat(prompt);
        return parseAiResponse(aiResponse);
    }

    private String buildRecommendationPrompt(RecommendationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a healthcare product recommendation assistant. ");
        sb.append("Use the user's request and any provided health context to recommend suitable products from the catalog and provide health tips.\n\n");

        sb.append("User context:\n");
        appendContextLine(sb, "Request", request.getQuery());
        appendContextLine(sb, "Symptoms", request.getSymptoms());
        appendContextLine(sb, "Current health conditions", request.getCurrentConditions());
        appendContextLine(sb, "Previous health conditions", request.getPreviousConditions());
        appendContextLine(sb, "Preferred category", request.getPreferredCategory());
        appendContextLine(sb, "Known allergies", request.getAllergies());
        appendContextLine(sb, "Recently browsed categories", request.getRecentlyBrowsedCategories());

        List<ProductRef> availableProducts = productService.getAllProductsForRecommendation();
        if (!availableProducts.isEmpty()) {
            sb.append("\nAvailable products in our catalog (recommend ONLY from this list):\n");
            for (ProductRef p : availableProducts) {
                sb.append("- ").append(p.name()).append("\n");
            }
            sb.append("\nIMPORTANT: You MUST recommend products from the above catalog only. ");
            sb.append("Use the EXACT product name as listed in the catalog.\n");
        }

        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 5;
        sb.append("\nPlease respond in the following JSON format only, no other text:\n");
        sb.append("{\n");
        sb.append("  \"reasoning\": \"brief explanation of recommendations\",\n");
        sb.append("  \"products\": [\n");
        sb.append("    {\"productName\": \"exact name from catalog\", \"category\": \"...\", \"description\": \"...\", \"reason\": \"why this product fits\", \"confidenceScore\": 0.0}\n");
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

    private void appendContextLine(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("- ").append(label).append(": ").append(value).append("\n");
        }
    }

    private void appendContextLine(StringBuilder sb, String label, List<String> values) {
        if (values != null && !values.isEmpty()) {
            sb.append("- ").append(label).append(": ").append(String.join(", ", values)).append("\n");
        }
    }

    private RecommendationResponse parseAiResponse(String aiResponse) {
        try {
            String json = extractJson(aiResponse);
            RecommendationResponse response = objectMapper.readValue(json, RecommendationResponse.class);

            if (response.getProducts() == null) {
                response.setProducts(new ArrayList<>());
            }
            if (response.getHealthTips() == null) {
                response.setHealthTips(new ArrayList<>());
            }

            // Match recommended products to real product IDs from database
            Map<String, Long> productNameToId = productService.getProductNameToIdMap();
            for (RecommendationResponse.RecommendedProduct rp : response.getProducts()) {
                String nameLower = rp.getProductName() != null ? rp.getProductName().toLowerCase() : "";
                Long matchedId = productNameToId.get(nameLower);
                if (matchedId != null) {
                    rp.setProductId(matchedId);
                    log.debug("Matched recommended product '{}' to database id={}", rp.getProductName(), matchedId);
                } else {
                    log.debug("No database match found for recommended product '{}'", rp.getProductName());
                }
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
