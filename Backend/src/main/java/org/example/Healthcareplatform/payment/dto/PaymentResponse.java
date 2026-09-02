package org.example.Healthcareplatform.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Healthcareplatform.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private BigDecimal amount;
    private Payment.Method method;
    private Payment.Status status;
    private String transactionId;
    private String receiptNumber;
    private Instant paidAt;
    private String errorMessage;
    private String billingName;
    private Instant createdAt;

    public static PaymentResponse fromEntity(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .orderNumber(p.getOrderNumber())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .method(p.getMethod())
                .status(p.getStatus())
                .transactionId(p.getTransactionId())
                .receiptNumber(p.getReceiptNumber())
                .paidAt(p.getPaidAt())
                .errorMessage(p.getErrorMessage())
                .billingName(p.getBillingName())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
