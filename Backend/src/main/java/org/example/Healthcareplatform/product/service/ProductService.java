package org.example.Healthcareplatform.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.product.dto.CategoryCountResponse;
import org.example.Healthcareplatform.product.dto.ProductRequest;
import org.example.Healthcareplatform.product.dto.ProductResponse;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(
            String keyword,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size) {

        Product.ProductCategory productCategory = null;
        if (category != null && !category.isBlank()) {
            try {
                productCategory = Product.ProductCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category: {}", category);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (keyword != null && !keyword.isBlank()) {
            Page<Product> products = productRepository.searchProducts(
                    keyword, productCategory, minPrice, maxPrice, pageable);
            return products.map(ProductResponse::fromEntity);
        }

        if (productCategory != null && minPrice != null && maxPrice != null) {
            return productRepository.findByCategoryAndPriceBetween(productCategory, minPrice, maxPrice, pageable)
                    .map(ProductResponse::fromEntity);
        }

        if (productCategory != null) {
            return productRepository.findByCategory(productCategory, pageable)
                    .map(ProductResponse::fromEntity);
        }

        if (minPrice != null && maxPrice != null) {
            return productRepository.findByPriceBetween(minPrice, maxPrice, pageable)
                    .map(ProductResponse::fromEntity);
        }

        Page<Product> products = productRepository.findAll(pageable);
        return products.map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be greater than zero");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new IllegalArgumentException("Product category is required");
        }

        Product.ProductCategory category;
        try {
            category = Product.ProductCategory.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid product category: " + request.getCategory());
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .imageUrl(request.getImageUrl())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .manufacturer(request.getManufacturer())
                .dosage(request.getDosage())
                .ingredients(request.getIngredients())
                .prescriptionRequired(request.getPrescriptionRequired() != null ? request.getPrescriptionRequired() : false)
                .sideEffects(request.getSideEffects())
                .ratings(0.0)
                .build();

        Product saved = productRepository.save(product);
        log.info("Created product id={}, name={}, category={}", saved.getId(), saved.getName(), saved.getCategory());
        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            product.setPrice(request.getPrice());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            try {
                product.setCategory(Product.ProductCategory.valueOf(request.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid product category: " + request.getCategory());
            }
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getManufacturer() != null) {
            product.setManufacturer(request.getManufacturer());
        }
        if (request.getDosage() != null) {
            product.setDosage(request.getDosage());
        }
        if (request.getIngredients() != null) {
            product.setIngredients(request.getIngredients());
        }
        if (request.getPrescriptionRequired() != null) {
            product.setPrescriptionRequired(request.getPrescriptionRequired());
        }
        if (request.getSideEffects() != null) {
            product.setSideEffects(request.getSideEffects());
        }

        Product saved = productRepository.save(product);
        log.info("Updated product id={}, name={}", saved.getId(), saved.getName());
        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        productRepository.deleteById(id);
        log.info("Deleted product id={}", id);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductsForRecommendation() {
        return productRepository.findAllByOrderByNameAsc().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryCountResponse> getCategoryCounts() {
        List<Object[]> results = productRepository.countProductsByCategory();
        return results.stream()
                .map(row -> {
                    Product.ProductCategory cat = (Product.ProductCategory) row[0];
                    long count = (Long) row[1];
                    return new CategoryCountResponse(cat.name(), count);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getProductNameToIdMap() {
        return productRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toMap(
                        p -> p.getName().toLowerCase(),
                        Product::getId,
                        (existing, replacement) -> existing));
    }
}
