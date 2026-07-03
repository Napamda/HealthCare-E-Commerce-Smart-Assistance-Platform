package org.example.Healthcareplatform.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {

    private Long userId;

    private String query;

    private String symptom;

    private String condition;

    private String category;

    private List<String> allergies;

    private List<String> recentProductCategories;

    private Integer maxResults;
}
