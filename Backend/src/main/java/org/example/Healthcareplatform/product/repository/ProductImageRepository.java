package org.example.Healthcareplatform.product.repository;

import org.example.Healthcareplatform.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    Optional<ProductImage> findFirstByProductIdAndPrimaryTrue(Long productId);

    long countByProductId(Long productId);

    @Modifying
    @Query("UPDATE ProductImage i SET i.primary = false WHERE i.productId = :productId")
    void clearPrimary(@Param("productId") Long productId);
}
