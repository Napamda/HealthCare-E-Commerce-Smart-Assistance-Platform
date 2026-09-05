package org.example.Healthcareplatform.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutPreviewResponse {
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountedSubtotal;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String shippingMethod;
    private String discountCode;
    private boolean hasPrescriptionRequired;
    private boolean discountValid;
    private String discountError;
    private int itemCount;
}