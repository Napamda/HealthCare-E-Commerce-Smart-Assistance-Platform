package org.example.Healthcareplatform.discount.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Healthcareplatform.discount.entity.DiscountCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResponse {

    private Long id;
    private String code;
    private DiscountCode.Type type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private Boolean active;
    private Instant expiresAt;
    private Integer usageLimit;
    private Integer timesUsed;
    private String description;
    private Instant createdAt;

    public static DiscountResponse fromEntity(DiscountCode d) {
        return DiscountResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .type(d.getType())
                .value(d.getValue())
                .minOrderAmount(d.getMinOrderAmount())
                .active(d.getActive())
                .expiresAt(d.getExpiresAt())
                .usageLimit(d.getUsageLimit())
                .timesUsed(d.getTimesUsed())
                .description(d.getDescription())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
