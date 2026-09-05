package org.example.Healthcareplatform.inventory.repository;

import org.example.Healthcareplatform.inventory.entity.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    List<StockHistory> findByProductIdOrderByCreatedAtDesc(Long productId);

    Page<StockHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
