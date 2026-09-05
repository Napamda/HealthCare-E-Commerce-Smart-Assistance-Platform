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

    private String query;

    private List<String> symptoms;

    private List<String> currentConditions;

    private List<String> previousConditions;

    private String preferredCategory;

    private List<String> allergies;

    private List<String> recentlyBrowsedCategories;

    private Integer maxResults;
}
