package org.example.Healthcareplatform.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.category.dto.CategoryRequest;
import org.example.Healthcareplatform.category.dto.CategoryResponse;
import org.example.Healthcareplatform.category.entity.Category;
import org.example.Healthcareplatform.category.repository.CategoryRepository;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    private String slugify(String name) {
        if (name == null) return "";
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree(boolean includeInactive) {
        List<Category> roots = includeInactive
                ? categoryRepository.findByParentIsNullOrderBySortOrderAsc()
                : categoryRepository.findByParentIsNullOrderBySortOrderAsc().stream()
                        .filter(Category::getActive)
                        .collect(Collectors.toList());
        return roots.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllWithParents().stream()
                .map(c -> toFlatResponse(c))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Category already exists: " + request.getName());
        }
        Category.CategoryBuilder builder = Category.builder()
                .name(request.getName().trim())
                .slug(resolveSlug(request))
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found: " + request.getParentId()));
            builder.parent(parent);
        }
        Category saved = categoryRepository.save(builder.build());
        log.info("Created category id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            category.setName(request.getName().trim());
        }
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            category.setSlug(request.getSlug());
        } else if (request.getName() != null && !request.getName().isBlank()) {
            category.setSlug(slugify(request.getName()));
        }
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getImageUrl() != null) category.setImageUrl(request.getImageUrl());
        if (request.getActive() != null) category.setActive(request.getActive());
        if (request.getSortOrder() != null) category.setSortOrder(request.getSortOrder());
        if (request.getParentId() != null) {
            if (request.getParentId().equals(category.getId())) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found: " + request.getParentId()));
            category.setParent(parent);
        }
        Category saved = categoryRepository.save(category);
        log.info("Updated category id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        if (categoryRepository.countByParentId(id) > 0) {
            throw new IllegalArgumentException("Cannot delete a category that has sub-categories");
        }
        categoryRepository.delete(category);
        log.info("Deleted category id={}", id);
    }

    private String resolveSlug(CategoryRequest request) {
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String slug = request.getSlug();
            if (categoryRepository.findBySlug(slug).isPresent()) {
                throw new IllegalArgumentException("Category slug already in use: " + slug);
            }
            return slug;
        }
        String base = slugify(request.getName());
        String slug = base;
        int n = 2;
        while (categoryRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + n++;
        }
        return slug;
    }

    private long countProducts(Category category) {
        String normalized = category.getName().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("(^_|_$)", "");
        try {
            Product.ProductCategory enumCategory = Product.ProductCategory.valueOf(normalized);
            return productRepository.countByCategory(enumCategory);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    private CategoryResponse toFlatResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .active(category.getActive())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .productCount(countProducts(category))
                .children(new ArrayList<>())
                .build();
    }

    private CategoryResponse toResponse(Category category) {
        List<CategoryResponse> childResponses = category.getChildren().stream()
                .filter(c -> c.getActive())
                .map(this::toResponse)
                .collect(Collectors.toList());
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .active(category.getActive())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .children(childResponses)
                .productCount(countProducts(category))
                .build();
    }
}
