package org.example.Healthcareplatform.product.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "stock_quantity")
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(length = 200)
    private String manufacturer;

    @Column(length = 200)
    private String dosage;

    @Column(columnDefinition = "TEXT")
    private String ingredients;

    @Column(name = "prescription_required")
    @Builder.Default
    private Boolean prescriptionRequired = false;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects;

    @Column(nullable = false)
    @Builder.Default
    private Double ratings = 0.0;

    @ElementCollection
    @CollectionTable(name = "product_reviews", joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum ProductCategory {
        VITAMINS,
        PAIN_RELIEF,
        SKIN_CARE,
        DIGESTIVE_HEALTH,
        RESPIRATORY,
        HEART_HEALTH,
        DIABETES_CARE,
        FIRST_AID,
        MEDICAL_DEVICES,
        PERSONAL_CARE,
        WELLNESS,
        BABY_CARE,
        ELDERLY_CARE,
        OTHER
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Review {
        @Column(name = "reviewer_name", length = 100)
        private String reviewerName;

        @Column(nullable = false)
        private Integer rating;

        @Column(columnDefinition = "TEXT")
        private String comment;

        @CreationTimestamp
        @Column(name = "created_at")
        private Instant createdAt;
    }
}
