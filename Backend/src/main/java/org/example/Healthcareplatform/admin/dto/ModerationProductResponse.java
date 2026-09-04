package org.example.Healthcareplatform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationProductResponse {
    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private String imageUrl;
    private String manufacturer;
    private Boolean prescriptionRequired;
    private String status;
    private String moderationReason;
    private Long moderatedBy;
    private Instant createdAt;
}
