package org.example.Healthcareplatform.product.repository;

import org.example.Healthcareplatform.product.dto.ProductRef;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.entity.ProductStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE " +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("category") Product.ProductCategory category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    Optional<Product> findByNameIgnoreCase(String name);

    List<Product> findByNameInIgnoreCase(List<String> names);

    @Query("SELECT new org.example.Healthcareplatform.product.dto.ProductRef(p.id, p.name) FROM Product p ORDER BY p.name ASC")
    @Cacheable(cacheNames = "products")
    List<ProductRef> findAllProductRefs();

    @Query("SELECT p.category as category, COUNT(p) as count FROM Product p GROUP BY p.category")
    List<Object[]> countProductsByCategory();

    long countByCategory(Product.ProductCategory category);

    // ---- Task 3.3 — Moderation queries ----
    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    long countByStatus(ProductStatus status);
}
