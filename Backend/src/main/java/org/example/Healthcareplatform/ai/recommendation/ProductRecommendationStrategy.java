package org.example.Healthcareplatform.ai.recommendation;

import org.example.Healthcareplatform.ai.dto.RecommendationRequest;
import org.example.Healthcareplatform.ai.dto.RecommendationResponse;

import java.util.List;

public interface ProductRecommendationStrategy {

    List<RecommendationResponse.RecommendedProduct> recommend(RecommendationRequest request);

    String strategyName();
}
