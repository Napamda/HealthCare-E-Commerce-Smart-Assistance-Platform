package org.example.Healthcareplatform.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.dto.RecommendationRequest;
import org.example.Healthcareplatform.ai.dto.RecommendationResponse;
import org.example.Healthcareplatform.ai.recommendation.AllergyChecker;
import org.example.Healthcareplatform.ai.recommendation.RecommendationEngine;
import org.example.Healthcareplatform.user.entity.HealthProfile;
import org.example.Healthcareplatform.user.repository.HealthProfileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationEngine recommendationEngine;
    private final AllergyChecker allergyChecker;
    private final HealthProfileRepository healthProfileRepository;

    public RecommendationResponse getRecommendations(RecommendationRequest request, Long userId) {
        log.info("RecommendationService.getRecommendations — userId={}, query={}",
                userId, request.getQuery());

        // Fetch user's health profile and inject into request
        HealthProfile healthProfile = healthProfileRepository.findByUserId(userId).orElse(null);
        if (healthProfile != null) {
            // Merge profile allergies with request allergies
            List<String> mergedAllergies = new ArrayList<>();
            if (request.getAllergies() != null) {
                mergedAllergies.addAll(request.getAllergies());
            }
            if (healthProfile.getAllergies() != null && !healthProfile.getAllergies().isEmpty()) {
                mergedAllergies.addAll(healthProfile.getAllergies());
            }
            request.setAllergies(mergedAllergies.isEmpty() ? null : mergedAllergies);

            // Merge profile chronic conditions with current conditions
            List<String> mergedConditions = new ArrayList<>();
            if (request.getCurrentConditions() != null) {
                mergedConditions.addAll(request.getCurrentConditions());
            }
            if (healthProfile.getChronicConditions() != null && !healthProfile.getChronicConditions().isEmpty()) {
                mergedConditions.addAll(healthProfile.getChronicConditions());
            }
            request.setCurrentConditions(mergedConditions.isEmpty() ? null : mergedConditions);

            log.info("Injected health profile — allergies={}, conditions={}",
                    request.getAllergies() != null ? request.getAllergies().size() : 0,
                    request.getCurrentConditions() != null ? request.getCurrentConditions().size() : 0);
        }

        RecommendationResponse response = recommendationEngine.generateRecommendations(request);

        List<String> allergyWarnings = allergyChecker.checkAllergies(request, response.getProducts());
        response.setAllergyWarnings(allergyWarnings);

        return response;
    }
}
