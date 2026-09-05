package org.example.Healthcareplatform.category.repository;

import org.example.Healthcareplatform.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByOrderByNameAsc();

    List<Category> findByParentIsNullOrderBySortOrderAsc();

    List<Category> findByActiveTrueOrderByNameAsc();

    long countByParentId(Long parentId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent ORDER BY c.name")
    List<Category> findAllWithParents();
}
