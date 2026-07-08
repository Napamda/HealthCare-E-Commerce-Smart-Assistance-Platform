package org.example.Healthcareplatform.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    private String reasoning;

    private List<RecommendedProduct> products;

    private List<HealthTip> healthTips;

    private List<String> allergyWarnings;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedProduct {
        private Long productId;
        private String productName;
        private String category;
        private String description;
        private String reason;
        private double confidenceScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthTip {
        private String title;
        private String content;
        private String category;
    }
}
