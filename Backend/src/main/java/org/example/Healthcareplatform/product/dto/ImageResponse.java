package org.example.Healthcareplatform.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Healthcareplatform.product.entity.ProductImage;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {

    private Long id;
    private Long productId;
    private String fileName;
    private String contentType;
    private String url;
    private String thumbnailUrl;
    private Boolean primary;
    private Integer sortOrder;
    private Instant createdAt;

    public static ImageResponse fromEntity(ProductImage image) {
        return ImageResponse.builder()
                .id(image.getId())
                .productId(image.getProductId())
                .fileName(image.getFileName())
                .contentType(image.getContentType())
                .url(image.getUrl())
                .thumbnailUrl(image.getThumbnailUrl())
                .primary(image.getPrimary())
                .sortOrder(image.getSortOrder())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
