package org.example.Healthcareplatform.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Healthcareplatform.order.entity.Order;
import org.example.Healthcareplatform.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userName;
    private List<OrderItemResponse> items;
    private String orderNumber;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String shippingMethod;
    private String discountCode;
    private String status;
    private String shippingAddress;
    private String shippingCity;
    private String shippingPhone;
    private String paymentMethod;
    private String trackingNumber;
    private String notes;
    private int itemCount;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    public static OrderResponse fromEntity(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .userName(order.getUserName())
                .items(order.getItems().stream()
                        .map(OrderResponse::toItemResponse)
                        .collect(Collectors.toList()))
                .orderNumber(order.getOrderNumber())
                .subtotalAmount(order.getSubtotalAmount())
                .shippingAmount(order.getShippingAmount())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .shippingMethod(order.getShippingMethod())
                .discountCode(order.getDiscountCode())
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingPhone(order.getShippingPhone())
                .paymentMethod(order.getPaymentMethod())
                .trackingNumber(order.getTrackingNumber())
                .notes(order.getNotes())
                .itemCount(order.getItems().stream()
                        .mapToInt(OrderItem::getQuantity).sum())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}
