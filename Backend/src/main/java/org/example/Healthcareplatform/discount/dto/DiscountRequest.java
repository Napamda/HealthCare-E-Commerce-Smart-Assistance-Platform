package org.example.Healthcareplatform.discount.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Type is required (PERCENTAGE or FIXED_AMOUNT)")
    private String type;

    @NotNull(message = "Value is required")
    @DecimalMin(value = "0.01", message = "Value must be greater than zero")
    private BigDecimal value;

    @DecimalMin(value = "0.00", message = "Min order amount cannot be negative")
    private BigDecimal minOrderAmount;

    private Boolean active;

    private Instant expiresAt;

    private Integer usageLimit;

    private String description;
}
