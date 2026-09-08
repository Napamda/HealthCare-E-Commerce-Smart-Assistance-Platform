package org.example.Healthcareplatform.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Healthcareplatform.payment.entity.Payment;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {
    private String method;
    private String displayName;
    private String description;
    private boolean requiresCardDetails;
    private boolean supportsRefunds;
    private double processingFee;
    private List<String> supportedCurrencies;
    private boolean enabled;

    public static PaymentMethodResponse fromMethod(Payment.Method method) {
        if (method == Payment.Method.CARD) {
            return PaymentMethodResponse.builder()
                    .method("CARD")
                    .displayName("Credit/Debit Card")
                    .description("Pay securely with your credit or debit card")
                    .requiresCardDetails(true)
                    .supportsRefunds(true)
                    .processingFee(0.0)
                    .supportedCurrencies(List.of("USD"))
                    .enabled(true)
                    .build();
        } else if (method == Payment.Method.PAYPAL) {
            return PaymentMethodResponse.builder()
                    .method("PAYPAL")
                    .displayName("PayPal")
                    .description("Pay with your PayPal account")
                    .requiresCardDetails(false)
                    .supportsRefunds(true)
                    .processingFee(0.0)
                    .supportedCurrencies(List.of("USD"))
                    .enabled(true)
                    .build();
        } else if (method == Payment.Method.BANK_TRANSFER) {
            return PaymentMethodResponse.builder()
                    .method("BANK_TRANSFER")
                    .displayName("Bank Transfer")
                    .description("Pay by bank transfer")
                    .requiresCardDetails(false)
                    .supportsRefunds(false)
                    .processingFee(0.0)
                    .supportedCurrencies(List.of("USD"))
                    .enabled(true)
                    .build();
        } else {
            return PaymentMethodResponse.builder()
                    .method("CASH_ON_DELIVERY")
                    .displayName("Cash on Delivery")
                    .description("Pay when your order is delivered")
                    .requiresCardDetails(false)
                    .supportsRefunds(false)
                    .processingFee(0.0)
                    .supportedCurrencies(List.of("USD"))
                    .enabled(true)
                    .build();
        }
    }
}