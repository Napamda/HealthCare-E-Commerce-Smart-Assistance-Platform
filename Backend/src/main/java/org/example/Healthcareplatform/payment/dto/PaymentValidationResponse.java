package org.example.Healthcareplatform.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentValidationResponse {
    private boolean valid;
    private String message;
    private List<String> errors;
    private boolean requiresAdditionalVerification;
    private String suggestedMethod;
}