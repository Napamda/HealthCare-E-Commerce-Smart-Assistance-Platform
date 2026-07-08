package org.example.Healthcareplatform.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    private String status;
    private String pharmacistComments;
    private Long pharmacistId;
}
