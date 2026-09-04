package org.example.Healthcareplatform.product.repository;

import org.example.Healthcareplatform.product.entity.Product;
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

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategory(Product.ProductCategory category, Pageable pageable);

    Page<Product> findByCategoryAndPriceBetween(
            Product.ProductCategory category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable);

    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

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

    List<Product> findAllByOrderByNameAsc();

    @Query("SELECT p.category as category, COUNT(p) as count FROM Product p GROUP BY p.category")
    List<Object[]> countProductsByCategory();

    long countByCategory(Product.ProductCategory category);
}
