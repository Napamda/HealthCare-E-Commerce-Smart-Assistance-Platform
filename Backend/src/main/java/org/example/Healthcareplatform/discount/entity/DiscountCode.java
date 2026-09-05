package org.example.Healthcareplatform.discount.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "discount_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCode {

    public enum Type {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "times_used")
    @Builder.Default
    private Integer timesUsed = 0;

    @Column(length = 300)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
