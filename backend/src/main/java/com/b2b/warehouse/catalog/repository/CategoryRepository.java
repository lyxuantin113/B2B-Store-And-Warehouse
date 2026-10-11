package com.b2b.warehouse.catalog.repository;

import com.b2b.warehouse.catalog.entity.CategoryEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    boolean existsBySlugIgnoreCase(String slug);

    Optional<CategoryEntity> findBySlugIgnoreCase(String slug);

    @EntityGraph(attributePaths = {"parent"})
    List<CategoryEntity> findAllByIsActiveTrueOrderBySortOrderAscNameAsc();

    @EntityGraph(attributePaths = {"parent"})
    List<CategoryEntity> findAllByOrderBySortOrderAscNameAsc();
}
