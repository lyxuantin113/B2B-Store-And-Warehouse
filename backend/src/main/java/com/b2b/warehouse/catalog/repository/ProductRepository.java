package com.b2b.warehouse.catalog.repository;

import com.b2b.warehouse.catalog.entity.ProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    @EntityGraph(attributePaths = {"category", "priceTiers"})
    Optional<ProductEntity> findWithDetailsByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"category", "priceTiers"})
    List<ProductEntity> findAllWithDetailsByIdInAndDeletedAtIsNull(java.util.Collection<UUID> ids);

    @EntityGraph(attributePaths = {"category", "priceTiers"})
    Optional<ProductEntity> findWithDetailsBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    /**
     * Initial page query fetching active products with Category and Dynamic Price Tiers
     * (No cursor parameters to prevent Postgres untyped null parameter inference issues).
     */
    @EntityGraph(attributePaths = {"category", "priceTiers"})
    @Query("""
            SELECT p FROM ProductEntity p
            WHERE p.isActive = TRUE
              AND p.deletedAt IS NULL
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (
                   :keyword IS NULL OR :keyword = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<ProductEntity> findActiveProductsInitial(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * Keyset pagination query fetching active products with Category and Dynamic Price Tiers
     * using explicit timestamp and UUID seek conditions.
     */
    @EntityGraph(attributePaths = {"category", "priceTiers"})
    @Query("""
            SELECT p FROM ProductEntity p
            WHERE p.isActive = TRUE
              AND p.deletedAt IS NULL
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (
                   :keyword IS NULL OR :keyword = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
              AND (
                   p.createdAt < :cursorCreatedAt
                   OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)
              )
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<ProductEntity> findActiveProductsKeyset(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
