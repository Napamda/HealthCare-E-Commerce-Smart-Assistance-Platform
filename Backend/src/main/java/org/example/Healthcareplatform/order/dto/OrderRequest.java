package org.example.Healthcareplatform.order.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private String shippingAddress;
    private String shippingCity;
    private String shippingPhone;
    private String paymentMethod;
    private String notes;
}
