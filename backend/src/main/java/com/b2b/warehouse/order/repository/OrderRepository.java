package com.b2b.warehouse.order.repository;

import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.OrderStatus;
import com.b2b.warehouse.order.entity.OrderType;
import com.b2b.warehouse.order.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    @EntityGraph(attributePaths = {"user", "branch", "items", "items.product"})
    Optional<OrderEntity> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"user", "branch", "items", "items.product"})
    Optional<OrderEntity> findWithDetailsByOrderCode(String orderCode);

    /**
     * Keyset pagination for B2B Partner Order History (Tab 4 of b2b-portal.php).
     * Uses composite index {@code idx_orders_user_type_created (user_id, order_type, created_at DESC, id DESC)}.
     */
    @EntityGraph(attributePaths = {"branch", "items"})
    @Query("""
            SELECT o FROM OrderEntity o
            WHERE o.user.id = :userId
              AND (:orderType IS NULL OR o.orderType = :orderType)
              AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
              AND (
                   :cursorCreatedAt IS NULL
                   OR o.createdAt < :cursorCreatedAt
                   OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId)
              )
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    List<OrderEntity> findPartnerOrdersKeyset(
            @Param("userId") UUID userId,
            @Param("orderType") OrderType orderType,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    /**
     * Keyset pagination for Admin Order Management &amp; Fulfillment (b2b-orders.php).
     * Uses composite index {@code idx_orders_status_payment_created}.
     */
    @EntityGraph(attributePaths = {"user", "branch", "items"})
    @Query("""
            SELECT o FROM OrderEntity o
            WHERE (:orderType IS NULL OR o.orderType = :orderType)
              AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
              AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
              AND (
                   :cursorCreatedAt IS NULL
                   OR o.createdAt < :cursorCreatedAt
                   OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId)
              )
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    List<OrderEntity> findAdminOrdersKeyset(
            @Param("orderType") OrderType orderType,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    /**
     * Finds stale UNPAID PENDING orders older than the cutoff timestamp (used by ShedLock Cron Job in Module 5).
     */
    @EntityGraph(attributePaths = {"branch", "items", "items.product"})
    @Query("""
            SELECT o FROM OrderEntity o
            WHERE o.orderStatus = 'PENDING'
              AND o.paymentStatus = 'UNPAID'
              AND o.createdAt < :cutoff
            """)
    List<OrderEntity> findExpiredUnpaidOrders(@Param("cutoff") Instant cutoff, Pageable pageable);

    // --- Partner Dashboard KPIs (Tab 1) ---
    long countByUser_Id(UUID userId);

    long countByUser_IdAndOrderType(UUID userId, OrderType orderType);

    long countByUser_IdAndOrderStatusIn(UUID userId, List<OrderStatus> statuses);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM OrderEntity o
            WHERE o.user.id = :userId
              AND o.paymentStatus = 'PAID'
              AND o.orderStatus NOT IN ('CANCELLED', 'REFUNDED')
            """)
    BigDecimal sumPaidSpendByUserId(@Param("userId") UUID userId);

    // --- Admin Sales Analytics KPIs (b2b-analytics.php) ---
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM OrderEntity o
            WHERE o.paymentStatus = 'PAID'
              AND o.orderStatus NOT IN ('CANCELLED', 'REFUNDED')
            """)
    BigDecimal sumTotalPlatformRevenue();

    long countByPaymentStatus(PaymentStatus paymentStatus);

    long countByOrderStatusIn(List<OrderStatus> statuses);
}
