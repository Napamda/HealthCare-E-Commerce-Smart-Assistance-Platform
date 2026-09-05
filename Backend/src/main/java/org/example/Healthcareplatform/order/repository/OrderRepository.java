package org.example.Healthcareplatform.order.repository;

import org.example.Healthcareplatform.order.entity.Order;
import org.example.Healthcareplatform.order.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR o.createdAt <= :endDate)")
    Page<Order> findUserOrdersWithFilters(
            @Param("userId") Long userId,
            @Param("status") Order.OrderStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Order.OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.userId = :userId AND o.status != :cancelledStatus")
    Double sumTotalAmountByUserId(@Param("userId") Long userId, @Param("cancelledStatus") Order.OrderStatus cancelledStatus);

    @Query("SELECT DISTINCT p.category FROM Order o JOIN o.items i JOIN org.example.Healthcareplatform.product.entity.Product p ON i.productId = p.id WHERE o.userId = :userId AND o.status != :cancelledStatus")
    List<String> findCategoriesByUserId(@Param("userId") Long userId, @Param("cancelledStatus") Order.OrderStatus cancelledStatus);
}
