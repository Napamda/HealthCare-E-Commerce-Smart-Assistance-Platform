package org.example.Healthcareplatform.inventory.dto;

import lombok.Data;
import java.util.List;

@Data
public class StockValidateResponse {

    private boolean valid;
    private List<ValidationError> errors;

    @Data
    public static class ValidationError {
        private Long productId;
        private String productName;
        private int requested;
        private long available;
        private String message;
    }
}