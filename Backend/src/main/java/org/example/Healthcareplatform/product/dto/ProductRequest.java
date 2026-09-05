package org.example.Healthcareplatform.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must be at most 200 characters")
    private String name;

    @Size(max = 10000, message = "Description is too long")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    @NotBlank(message = "Category is required")
    private String category;

    @Size(max = 500, message = "Image URL is too long")
    private String imageUrl;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;

    @Size(max = 200, message = "Manufacturer is too long")
    private String manufacturer;

    @Size(max = 200, message = "Dosage is too long")
    private String dosage;

    @Size(max = 10000, message = "Ingredients are too long")
    private String ingredients;

    private Boolean prescriptionRequired;

    @Size(max = 10000, message = "Side effects are too long")
    private String sideEffects;
}
