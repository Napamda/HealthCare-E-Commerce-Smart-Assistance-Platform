package org.example.Healthcareplatform.event.repository;

import org.example.Healthcareplatform.event.entity.EventStatus;
import org.example.Healthcareplatform.event.entity.HealthEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HealthEventRepository extends JpaRepository<HealthEvent, Long> {

    List<HealthEvent> findAllByOrderByStartDateTimeAsc();

    List<HealthEvent> findByStatusOrderByStartDateTimeAsc(EventStatus status);

    List<HealthEvent> findByCategoryIgnoreCaseOrderByStartDateTimeAsc(String category);

    List<HealthEvent> findByStatusAndCategoryIgnoreCaseOrderByStartDateTimeAsc(EventStatus status, String category);

    @Query("select distinct e.category from HealthEvent e order by e.category")
    List<String> findDistinctCategories();
}
