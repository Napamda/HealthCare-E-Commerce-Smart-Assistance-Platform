package org.example.Healthcareplatform.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModerationDecisionRequest {

    /** APPROVED or REJECTED (for vendors/products) or PUBLISHED/CANCELLED (for events). */
    private String decision;

    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;
}
