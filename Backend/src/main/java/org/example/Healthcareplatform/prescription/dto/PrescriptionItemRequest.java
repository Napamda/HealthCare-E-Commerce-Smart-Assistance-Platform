package org.example.Healthcareplatform.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A medication the pharmacist selected while approving a prescription.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItemRequest {

    private Long productId;
    private String productName;
    private Integer quantity;
    private String dosageInstructions;
}
