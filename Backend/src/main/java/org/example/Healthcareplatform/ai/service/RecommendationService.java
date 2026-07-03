package org.example.Healthcareplatform.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.dto.RecommendationRequest;
import org.example.Healthcareplatform.ai.dto.RecommendationResponse;
import org.example.Healthcareplatform.ai.recommendation.AllergyChecker;
import org.example.Healthcareplatform.ai.recommendation.RecommendationEngine;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationEngine recommendationEngine;
    private final AllergyChecker allergyChecker;

    public RecommendationResponse getRecommendations(RecommendationRequest request) {
        log.info("RecommendationService.getRecommendations — userId={}, query={}",
                request.getUserId(), request.getQuery());

        RecommendationResponse response = recommendationEngine.generateRecommendations(request);

        List<String> allergyWarnings = allergyChecker.checkAllergies(request, response.getProducts());
        response.setAllergyWarnings(allergyWarnings);

        return response;
    }
}
