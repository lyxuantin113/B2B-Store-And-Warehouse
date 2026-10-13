package com.b2b.warehouse.async;

import com.b2b.warehouse.inventory.entity.BranchInventoryEntity;
import com.b2b.warehouse.inventory.repository.BranchInventoryRepository;
import com.b2b.warehouse.inventory.service.InventoryService;
import com.b2b.warehouse.notification.service.NotificationAndAnnouncementService;
import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.OrderItemEntity;
import com.b2b.warehouse.order.entity.OrderStatus;
import com.b2b.warehouse.order.repository.OrderRepository;
import com.b2b.warehouse.outbox.entity.OutboxEventEntity;
import com.b2b.warehouse.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Distributed Background Cron Jobs protected by ShedLock (PostgreSQL {@code shedlock} table)
 * to guarantee single-node execution across a multi-pod Kubernetes cluster:
 * <ul>
 *   <li><b>Job 1 (Every 15 mins)</b>: Auto-cancels stale {@code PENDING} + {@code UNPAID} orders older than 24 hours
 *       and atomically releases reserved warehouse stock back to {@code available_qty}.</li>
 *   <li><b>Job 2 (Every 6 hours)</b>: Scans active branches for SKUs at or below {@code safety_stock}
 *       and emits a {@code LOW_STOCK_SAFETY_ALERT} outbox event.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseMaintenanceCronScheduler {

    private static final Duration UNPAID_ORDER_EXPIRY_DURATION = Duration.ofHours(24);
    private static final int EXPIRY_BATCH_SIZE = 50;

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final BranchInventoryRepository branchInventoryRepository;
    private final NotificationAndAnnouncementService notificationService;
    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(cron = "${app.cron.stale-order-cleanup:0 */15 * * * *}")
    @SchedulerLock(
            name = "staleUnpaidOrderCleanupLock",
            lockAtLeastFor = "PT1M",
            lockAtMostFor = "PT10M"
    )
    @Transactional
    public void cancelExpiredUnpaidOrdersAndReleaseStock() {
        Instant cutoff = Instant.now().minus(UNPAID_ORDER_EXPIRY_DURATION);
        List<OrderEntity> expiredOrders = orderRepository.findExpiredUnpaidOrders(
                cutoff,
                PageRequest.of(0, EXPIRY_BATCH_SIZE)
        );
        if (expiredOrders.isEmpty()) {
            return;
        }

        int cancelledCount = 0;
        for (OrderEntity order : expiredOrders) {
            if (order.getBranch() != null && order.getItems() != null && !order.getItems().isEmpty()) {
                List<com.b2b.warehouse.inventory.dto.StockReservationItemDto> releaseItems = order.getItems().stream()
                        .map(item -> new com.b2b.warehouse.inventory.dto.StockReservationItemDto(
                                item.getProduct().getId(),
                                item.getQuantity()
                        ))
                        .toList();
                inventoryService.releaseReservedStockForOrder(order.getBranch().getId(), releaseItems);
            }

            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(Instant.now());
            order.setAdminMemo("Auto-cancelled by ShedLock Cron Job after 24h payment timeout");
            orderRepository.save(order);

            if (order.getUser() != null) {
                notificationService.createPersonalNotification(
                        order.getUser().getId(),
                        "ORDER_EXPIRED",
                        "Order Expired: " + order.getOrderCode(),
                        "Order " + order.getOrderCode() + " was automatically cancelled after 24 hours without payment, and reserved stock has been released.",
                        order.getId()
                );
            }
            cancelledCount++;
        }

        log.info("ShedLock Cron Job auto-cancelled {} expired unpaid orders and released reserved stock", cancelledCount);
    }

    @Scheduled(cron = "${app.cron.low-stock-alert:0 0 */6 * * *}")
    @SchedulerLock(
            name = "lowStockSafetyAlertLock",
            lockAtLeastFor = "PT5M",
            lockAtMostFor = "PT30M"
    )
    @Transactional
    public void scanLowStockAndEmitSafetyAlerts() {
        List<BranchInventoryEntity> lowStockItems = branchInventoryRepository.findLowStockAlerts();
        if (lowStockItems.isEmpty()) {
            return;
        }

        List<Map<String, Object>> alertEntries = lowStockItems.stream()
                .limit(50)
                .map(bi -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("branchCode", bi.getBranch().getCode());
                    item.put("branchName", bi.getBranch().getName());
                    item.put("productSku", bi.getProduct().getSku());
                    item.put("productName", bi.getProduct().getName());
                    item.put("availableQty", bi.getAvailableQty());
                    item.put("safetyStock", bi.getSafetyStock());
                    return item;
                })
                .toList();

        OutboxEventEntity alertEvent = OutboxEventEntity.builder()
                .aggregateType("INVENTORY")
                .aggregateId("LOW_STOCK_BATCH")
                .eventType("LOW_STOCK_SAFETY_ALERT")
                .payload(Map.of(
                        "totalLowStockSkus", lowStockItems.size(),
                        "items", alertEntries,
                        "detectedAt", Instant.now().toString()
                ))
                .status("PENDING")
                .build();

        outboxEventRepository.save(alertEvent);
        log.warn("ShedLock Low-Stock Cron detected {} branch SKUs at or below safety_stock threshold", lowStockItems.size());
    }
}
