package org.example.Healthcareplatform.ai.recommendation;

import org.example.Healthcareplatform.ai.dto.RecommendationRequest;
import org.example.Healthcareplatform.ai.dto.RecommendationResponse;
import org.example.Healthcareplatform.ai.provider.AIProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AllergyChecker {

    private static final Set<String> COMMON_ALLERGENS = Set.of(
            "peanut", "peanuts", "tree nut", "almond", "walnut", "cashew",
            "milk", "lactose", "dairy", "whey", "casein",
            "egg", "eggs", "egg white", "egg yolk",
            "soy", "soya", "soybean",
            "wheat", "gluten", "barley", "rye",
            "shellfish", "shrimp", "crab", "lobster",
            "fish", "cod", "salmon", "tuna",
            "sesame", "mustard", "sulfite"
    );

    public List<String> checkAllergies(RecommendationRequest request,
                                        List<RecommendationResponse.RecommendedProduct> products) {
        List<String> warnings = new ArrayList<>();
        List<String> userAllergies = request.getAllergies();

        if (userAllergies == null || userAllergies.isEmpty()) {
            return warnings;
        }

        Set<String> userAllergensLower = new HashSet<>();
        for (String allergy : userAllergies) {
            userAllergensLower.add(allergy.toLowerCase().trim());
        }

        for (RecommendationResponse.RecommendedProduct product : products) {
            String desc = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
            String name = product.getProductName() != null ? product.getProductName().toLowerCase() : "";

            for (String allergen : userAllergensLower) {
                if (desc.contains(allergen) || name.contains(allergen)) {
                    warnings.add(String.format(
                            "Allergy warning: '%s' may contain '%s' — %s",
                            product.getProductName(), allergen, product.getReason()));
                    break;
                }
            }

            for (String commonAllergen : COMMON_ALLERGENS) {
                if (userAllergensLower.contains(commonAllergen)) {
                    continue;
                }
                if (desc.contains(commonAllergen) || name.contains(commonAllergen)) {
                    warnings.add(String.format(
                            "Note: '%s' lists '%s' as an ingredient — verify if you have this allergy",
                            product.getProductName(), commonAllergen));
                }
            }
        }

        return warnings;
    }
}
