package org.example.Healthcareplatform.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be at most 100 characters")
    private String name;

    @Size(max = 120, message = "Slug must be at most 120 characters")
    private String slug;

    @Size(max = 5000, message = "Description is too long")
    private String description;

    private String imageUrl;

    private Long parentId;

    private Boolean active;

    private Integer sortOrder;
}
