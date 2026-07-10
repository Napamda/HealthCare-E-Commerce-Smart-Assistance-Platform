package org.example.Healthcareplatform.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartRequest {
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal unitPrice;
}
