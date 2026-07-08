package org.example.Healthcareplatform.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Healthcareplatform.product.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private Integer stockQuantity;
    private String manufacturer;
    private String dosage;
    private String ingredients;
    private Boolean prescriptionRequired;
    private String sideEffects;
    private Double ratings;
    private List<ReviewResponse> reviews;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewResponse {
        private String reviewerName;
        private Integer rating;
        private String comment;
        private Instant createdAt;
    }

    public static ProductResponse fromEntity(Product product) {
        List<ReviewResponse> reviewResponses = product.getReviews().stream()
                .map(r -> ReviewResponse.builder()
                        .reviewerName(r.getReviewerName())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory().name())
                .imageUrl(product.getImageUrl())
                .stockQuantity(product.getStockQuantity())
                .manufacturer(product.getManufacturer())
                .dosage(product.getDosage())
                .ingredients(product.getIngredients())
                .prescriptionRequired(product.getPrescriptionRequired())
                .sideEffects(product.getSideEffects())
                .ratings(product.getRatings())
                .reviews(reviewResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
