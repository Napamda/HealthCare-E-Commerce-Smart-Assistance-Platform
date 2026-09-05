package org.example.Healthcareplatform.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdRequest {

    @NotNull(message = "Threshold is required")
    @Min(value = 0, message = "Threshold cannot be negative")
    private Integer threshold;
}
