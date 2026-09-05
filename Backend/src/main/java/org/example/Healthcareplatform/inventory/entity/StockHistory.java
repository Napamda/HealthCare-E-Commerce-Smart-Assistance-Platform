package org.example.Healthcareplatform.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "stock_history", indexes = {
        @Index(name = "idx_stock_history_product", columnList = "product_id"),
        @Index(name = "idx_stock_history_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockHistory {

    public enum ChangeType {
        INIT,
        PURCHASE,
        SALE,
        RESERVED,
        RESERVATION_RELEASED,
        RESERVATION_EXPIRED,
        ADJUSTMENT,
        CANCELLATION,
        REFUND
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 40)
    private ChangeType changeType;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
