package org.example.Healthcareplatform.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Long parentId;
    private Boolean active;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
    @Builder.Default
    private List<CategoryResponse> children = new ArrayList<>();
    private long productCount;
}
