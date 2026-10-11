package com.b2b.warehouse.inventory.repository;

import com.b2b.warehouse.inventory.entity.BranchInventoryEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchInventoryRepository extends JpaRepository<BranchInventoryEntity, Long> {

    interface ProductStockTotalProjection {
        UUID getProductId();
        Integer getTotalAvailable();
    }

    @EntityGraph(attributePaths = {"branch", "product"})
    Optional<BranchInventoryEntity> findByBranch_IdAndProduct_Id(UUID branchId, UUID productId);

    @EntityGraph(attributePaths = {"branch", "product"})
    List<BranchInventoryEntity> findAllByBranch_IdOrderByProduct_NameAsc(UUID branchId);

    @EntityGraph(attributePaths = {"branch", "product"})
    List<BranchInventoryEntity> findAllByProduct_IdOrderByBranch_NameAsc(UUID productId);

    /**
     * Finds all branch inventory records where available stock has dropped to or below safety stock threshold.
     */
    @EntityGraph(attributePaths = {"branch", "product"})
    @Query("""
            SELECT bi FROM BranchInventoryEntity bi
            JOIN bi.branch b
            JOIN bi.product p
            WHERE b.isActive = TRUE
              AND p.isActive = TRUE
              AND p.deletedAt IS NULL
              AND bi.availableQty <= bi.safetyStock
            ORDER BY bi.availableQty ASC
            """)
    List<BranchInventoryEntity> findLowStockAlerts();

    /**
     * Batch-aggregates total available stock across active branches for a set of products in 1 SQL query.
     * Prevents N+1 queries when rendering B2B Catalog &amp; Quick Order Matrix.
     */
    @Query("""
            SELECT bi.product.id AS productId, COALESCE(SUM(bi.availableQty), 0) AS totalAvailable
            FROM BranchInventoryEntity bi
            JOIN bi.branch b
            WHERE b.isActive = TRUE
              AND bi.product.id IN :productIds
            GROUP BY bi.product.id
            """)
    List<ProductStockTotalProjection> sumAvailableStockByProductIds(@Param("productIds") Collection<UUID> productIds);

    /**
     * ATOMIC STOCK RESERVATION (Zero Race Condition / Zero Overselling):
     * Atomically decrements {@code available_qty} and increments {@code reserved_qty} in a single PostgreSQL
     * row-level lock statement ONLY IF {@code available_qty >= :qty}.
     *
     * @return 1 if stock was reserved; 0 if insufficient stock or row does not exist
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BranchInventoryEntity bi
            SET bi.availableQty = bi.availableQty - :qty,
                bi.reservedQty  = bi.reservedQty + :qty,
                bi.version      = bi.version + 1,
                bi.updatedAt    = CURRENT_TIMESTAMP
            WHERE bi.branch.id = :branchId
              AND bi.product.id = :productId
              AND bi.availableQty >= :qty
            """)
    int reserveStockAtomically(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            @Param("qty") int qty
    );

    /**
     * ATOMIC RESERVATION RELEASE:
     * Restores {@code available_qty} and decrements {@code reserved_qty} when an unpaid order expires or is cancelled.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BranchInventoryEntity bi
            SET bi.availableQty = bi.availableQty + :qty,
                bi.reservedQty  = bi.reservedQty - :qty,
                bi.version      = bi.version + 1,
                bi.updatedAt    = CURRENT_TIMESTAMP
            WHERE bi.branch.id = :branchId
              AND bi.product.id = :productId
              AND bi.reservedQty >= :qty
            """)
    int releaseReservedStockAtomically(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            @Param("qty") int qty
    );

    /**
     * ATOMIC FULFILLMENT DEDUCTION:
     * Permanently deducts {@code reserved_qty} once an order is shipped/dispatched from the warehouse.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BranchInventoryEntity bi
            SET bi.reservedQty = bi.reservedQty - :qty,
                bi.version     = bi.version + 1,
                bi.updatedAt   = CURRENT_TIMESTAMP
            WHERE bi.branch.id = :branchId
              AND bi.product.id = :productId
              AND bi.reservedQty >= :qty
            """)
    int confirmReservedStockDeductionAtomically(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            @Param("qty") int qty
    );
}
