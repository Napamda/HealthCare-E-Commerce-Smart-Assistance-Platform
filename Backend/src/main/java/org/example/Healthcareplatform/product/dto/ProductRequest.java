package org.example.Healthcareplatform.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

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
}
