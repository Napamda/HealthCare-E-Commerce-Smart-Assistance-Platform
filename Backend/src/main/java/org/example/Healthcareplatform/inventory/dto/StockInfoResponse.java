package org.example.Healthcareplatform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Healthcareplatform.inventory.entity.StockHistory;
import org.example.Healthcareplatform.inventory.entity.StockReservation;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoResponse {

    private Long productId;
    private String productName;
    private String category;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Long reservedQuantity;
    private Long availableQuantity;
    private boolean lowStock;
    private boolean outOfStock;
    private List<StockHistoryDto> history;
    private List<StockReservationDto> reservations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockHistoryDto {
        private Long id;
        private Long productId;
        private String productName;
        private StockHistory.ChangeType changeType;
        private Integer quantityChange;
        private Integer stockAfter;
        private Long orderId;
        private String note;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockReservationDto {
        private Long id;
        private Long productId;
        private Long userId;
        private Integer quantity;
        private Long orderId;
        private StockReservation.Status status;
        private Instant expiresAt;
        private Instant createdAt;
    }
}
