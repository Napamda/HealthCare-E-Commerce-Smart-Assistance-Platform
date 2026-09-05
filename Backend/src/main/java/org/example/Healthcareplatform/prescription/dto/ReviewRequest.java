package org.example.Healthcareplatform.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    private String status;
    private String pharmacistComments;
    private Long pharmacistId;

    /**
     * Medications the pharmacist selected for the patient when approving.
     * Sent with an APPROVED review so the patient can order them directly.
     * Ignored for REJECTED reviews.
     */
    private List<PrescriptionItemRequest> items;
}
