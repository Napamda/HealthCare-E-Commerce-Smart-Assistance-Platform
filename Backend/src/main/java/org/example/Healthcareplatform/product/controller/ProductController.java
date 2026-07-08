package org.example.Healthcareplatform.product.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.product.dto.CategoryCountResponse;
import org.example.Healthcareplatform.product.dto.ProductResponse;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        log.info("GET /api/products — page={}, size={}", page, size);

        Page<ProductResponse> productPage = productService.listProducts(page, size);

        Map<String, Object> body = Map.of(
                "content", productPage.getContent(),
                "page", productPage.getNumber(),
                "size", productPage.getSize(),
                "totalElements", productPage.getTotalElements(),
                "totalPages", productPage.getTotalPages(),
                "first", productPage.isFirst(),
                "last", productPage.isLast()
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        log.info("GET /api/products/search — keyword={}, category={}, minPrice={}, maxPrice={}, page={}, size={}",
                keyword, category, minPrice, maxPrice, page, size);

        Page<ProductResponse> productPage = productService.searchProducts(
                keyword, category, minPrice, maxPrice, page, size);

        Map<String, Object> body = Map.of(
                "content", productPage.getContent(),
                "page", productPage.getNumber(),
                "size", productPage.getSize(),
                "totalElements", productPage.getTotalElements(),
                "totalPages", productPage.getTotalPages(),
                "first", productPage.isFirst(),
                "last", productPage.isLast()
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        log.info("GET /api/products/{}", id);

        try {
            ProductResponse response = productService.getProductById(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        log.info("GET /api/products/categories");
        List<String> categories = Arrays.stream(Product.ProductCategory.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/categories/with-counts")
    public ResponseEntity<List<CategoryCountResponse>> getCategoriesWithCounts() {
        log.info("GET /api/products/categories/with-counts");
        List<CategoryCountResponse> counts = productService.getCategoryCounts();
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/recommended/context")
    public ResponseEntity<List<ProductResponse>> getProductsForRecommendation() {
        log.info("GET /api/products/recommended/context");
        List<ProductResponse> products = productService.getAllProductsForRecommendation();
        return ResponseEntity.ok(products);
    }
}
