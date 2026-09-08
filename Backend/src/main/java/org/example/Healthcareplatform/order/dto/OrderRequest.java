package org.example.Healthcareplatform.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Shipping city is required")
    private String shippingCity;

    @NotBlank(message = "Shipping phone is required")
    @Size(max = 30, message = "Phone number is too long")
    private String shippingPhone;

    private String shippingMethod;

    private String discountCode;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "CARD|PAYPAL|BANK_TRANSFER|CASH_ON_DELIVERY", message = "Unsupported payment method")
    private String paymentMethod;

    private Boolean confirmPrescription;

    private String notes;
}
