package org.example.Healthcareplatform.inventory.repository;

import org.example.Healthcareplatform.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderId(Long orderId);

    List<StockReservation> findByOrderIdAndStatus(Long orderId, StockReservation.Status status);

    List<StockReservation> findByStatusAndExpiresAtBefore(StockReservation.Status status, Instant now);

    Optional<StockReservation> findByOrderIdAndProductIdAndStatus(Long orderId, Long productId, StockReservation.Status status);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM StockReservation r " +
            "WHERE r.productId = :productId AND r.status = 'ACTIVE'")
    Long sumActiveReservedQuantity(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM StockReservation r " +
            "WHERE r.productId = :productId AND r.status IN ('ACTIVE', 'CONFIRMED')")
    Long sumReservedQuantity(@Param("productId") Long productId);

    long countByStatusAndExpiresAtBefore(StockReservation.Status status, Instant now);
}
