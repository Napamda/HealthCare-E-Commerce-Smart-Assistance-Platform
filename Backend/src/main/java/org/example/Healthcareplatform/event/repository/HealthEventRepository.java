package org.example.Healthcareplatform.event.repository;

import org.example.Healthcareplatform.event.entity.EventStatus;
import org.example.Healthcareplatform.event.entity.HealthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthEventRepository extends JpaRepository<HealthEvent, Long> {

    List<HealthEvent> findAllByOrderByStartDateTimeAsc();

    List<HealthEvent> findByStatusOrderByStartDateTimeAsc(EventStatus status);

    List<HealthEvent> findByStatusOrderByCreatedAtDesc(EventStatus status);

    long countByStatus(EventStatus status);

    List<HealthEvent> findByCategoryIgnoreCaseOrderByStartDateTimeAsc(String category);

    List<HealthEvent> findByStatusAndCategoryIgnoreCaseOrderByStartDateTimeAsc(EventStatus status, String category);

    @Query("select distinct e.category from HealthEvent e order by e.category")
    List<String> findDistinctCategories();

    @Query("select distinct e.city from HealthEvent e where e.city is not null and e.city <> '' order by e.city")
    List<String> findDistinctCities();

    @Query("select e from HealthEvent e where "
            + "(:keyword is null or lower(e.title) like lower(concat('%', :keyword, '%')) "
            + "or lower(e.description) like lower(concat('%', :keyword, '%')) "
            + "or lower(e.venue) like lower(concat('%', :keyword, '%')) "
            + "or lower(e.city) like lower(concat('%', :keyword, '%')) "
            + "or lower(e.organizer) like lower(concat('%', :keyword, '%')) "
            + "or lower(e.category) like lower(concat('%', :keyword, '%')) "
            + "or exists (select 1 from e.tags t where lower(t) like lower(concat('%', :keyword, '%')))) "
            + "and (:category is null or lower(e.category) = lower(:category)) "
            + "and (:city is null or lower(e.city) = lower(:city)) "
            + "and (:status is null or e.status = :status) "
            + "and (:dateFrom is null or e.startDateTime >= :dateFrom) "
            + "and (:dateTo is null or e.startDateTime <= :dateTo)")
    Page<HealthEvent> searchEvents(@Param("keyword") String keyword,
                                   @Param("category") String category,
                                   @Param("city") String city,
                                   @Param("dateFrom") LocalDateTime dateFrom,
                                   @Param("dateTo") LocalDateTime dateTo,
                                   @Param("status") EventStatus status,
                                   Pageable pageable);
}
