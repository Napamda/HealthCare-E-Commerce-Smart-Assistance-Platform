package org.example.Healthcareplatform.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceiptDTO {
    private String receiptNumber;
    private String transactionId;
    private String orderNumber;
    private Long orderId;
    private Long userId;
    private String userName;
    private String userEmail;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String billingName;
    private Instant paidAt;
    private Instant createdAt;
    private List<ReceiptItem> items;
    private BigDecimal subtotal;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String shippingCity;
    private String shippingPhone;
    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptItem {
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}