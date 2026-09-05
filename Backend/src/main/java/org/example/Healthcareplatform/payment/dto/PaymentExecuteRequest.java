package org.example.Healthcareplatform.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentExecuteRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^\\d{13,19}$", message = "Card number must contain 13-19 digits")
    private String cardNumber;

    @NotBlank(message = "Card holder name is required")
    @Size(max = 150, message = "Card holder name is too long")
    private String cardHolder;

    @NotBlank(message = "Expiry month is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month must be MM")
    private String expiryMonth;

    @NotBlank(message = "Expiry year is required")
    @Pattern(regexp = "^\\d{4}$", message = "Expiry year must be YYYY")
    private String expiryYear;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3-4 digits")
    private String cvv;
}
