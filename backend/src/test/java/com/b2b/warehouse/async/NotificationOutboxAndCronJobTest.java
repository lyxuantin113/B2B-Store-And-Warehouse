package com.b2b.warehouse.async;

import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.catalog.entity.ProductEntity;
import com.b2b.warehouse.common.util.HtmlSanitizerUtil;
import com.b2b.warehouse.inventory.repository.BranchInventoryRepository;
import com.b2b.warehouse.inventory.service.InventoryService;
import com.b2b.warehouse.notification.dto.AnnouncementResponseDto;
import com.b2b.warehouse.notification.dto.UnifiedNotificationCenterDto;
import com.b2b.warehouse.notification.dto.UpsertAnnouncementRequestDto;
import com.b2b.warehouse.notification.entity.AnnouncementEntity;
import com.b2b.warehouse.notification.entity.NotificationEntity;
import com.b2b.warehouse.notification.repository.AnnouncementRepository;
import com.b2b.warehouse.notification.repository.NotificationRepository;
import com.b2b.warehouse.notification.service.NotificationAndAnnouncementService;
import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.OrderItemEntity;
import com.b2b.warehouse.order.entity.OrderStatus;
import com.b2b.warehouse.order.entity.PaymentStatus;
import com.b2b.warehouse.order.repository.OrderRepository;
import com.b2b.warehouse.outbox.entity.OutboxEventEntity;
import com.b2b.warehouse.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxAndCronJobTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private BranchInventoryRepository branchInventoryRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private NotificationAndAnnouncementService notificationService;
    private DomainEventNotificationProcessor notificationProcessor;
    private OutboxEventRelayScheduler outboxEventRelayScheduler;
    private WarehouseMaintenanceCronScheduler cronScheduler;

    @BeforeEach
    void setUp() {
        HtmlSanitizerUtil htmlSanitizerUtil = new HtmlSanitizerUtil();
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        notificationService = new NotificationAndAnnouncementService(
                announcementRepository,
                notificationRepository,
                htmlSanitizerUtil
        );
        notificationProcessor = new DomainEventNotificationProcessor(notificationService);
        outboxEventRelayScheduler = new OutboxEventRelayScheduler(
                outboxEventRepository,
                kafkaTemplate,
                notificationProcessor,
                objectMapper
        );
        cronScheduler = new WarehouseMaintenanceCronScheduler(
                orderRepository,
                inventoryService,
                branchInventoryRepository,
                notificationService,
                outboxEventRepository
        );
    }

    @Test
    @DisplayName("Broadcast Announcement creation sanitizes HTML and Unified Notification Center aggregates unread count + feed")
    void shouldSanitizeAnnouncementAndReturnUnifiedNotificationCenter() {
        when(announcementRepository.save(any(AnnouncementEntity.class)))
                .thenAnswer(inv -> {
                    AnnouncementEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        AnnouncementResponseDto created = notificationService.createAnnouncement(
                UUID.randomUUID(),
                new UpsertAnnouncementRequestDto(
                        "Holiday Warehouse Schedule",
                        "<p>Closed on National Day</p><img src=x onerror=alert(1)>",
                        "URGENT",
                        true,
                        true
                )
        );

        assertThat(created.contentHtml()).contains("<p>Closed on National Day</p>");
        assertThat(created.contentHtml()).doesNotContain("onerror");

        UUID partnerUserId = UUID.randomUUID();
        when(notificationRepository.countByUserIdAndIsReadFalse(partnerUserId)).thenReturn(3L);
        when(notificationRepository.findUserNotificationsKeyset(eq(partnerUserId), anyBoolean(), any(), any(), any()))
                .thenReturn(List.of(
                        NotificationEntity.builder()
                                .id(UUID.randomUUID())
                                .userId(partnerUserId)
                                .notifType("ORDER_STATUS")
                                .title("Order Shipped")
                                .message("Your order is on the way")
                                .isRead(false)
                                .createdAt(Instant.now())
                                .build()
                ));
        when(announcementRepository.findAllByIsPublishedTrueAndDeletedAtIsNullOrderByIsPinnedDescPublishedAtDesc())
                .thenReturn(List.of(
                        AnnouncementEntity.builder()
                                .id(created.id())
                                .title(created.title())
                                .contentHtml(created.contentHtml())
                                .categoryBadge("URGENT")
                                .isPinned(true)
                                .isPublished(true)
                                .publishedAt(Instant.now())
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build()
                ));

        UnifiedNotificationCenterDto feed = notificationService.getUnifiedNotificationCenter(
                partnerUserId, false, null, null, 10
        );

        assertThat(feed.unreadCount()).isEqualTo(3L);
        assertThat(feed.personalNotifications().items()).hasSize(1);
        assertThat(feed.broadcastAnnouncements()).hasSize(1);
    }

    @Test
    @DisplayName("OutboxEventRelayScheduler relays PENDING events, materializes Partner Notification, and marks event PUBLISHED")
    void shouldRelayPendingOutboxEventAndMarkPublished() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OutboxEventEntity pendingEvent = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(orderId.toString())
                .eventType("ORDER_CREATED")
                .payload(Map.of(
                        "orderCode", "WHL-20261011-AA11BB",
                        "userId", userId.toString()
                ))
                .status("PENDING")
                .retryCount(0)
                .createdAt(Instant.now())
                .build();

        when(outboxEventRepository.findPendingEventsBatch(any())).thenReturn(List.of(pendingEvent));
        when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        outboxEventRelayScheduler.relayPendingOutboxEvents();

        assertThat(pendingEvent.getStatus()).isEqualTo("PUBLISHED");
        assertThat(pendingEvent.getPublishedAt()).isNotNull();
        verify(notificationRepository).save(any(NotificationEntity.class));
    }

    @Test
    @DisplayName("ShedLock Cron Job cancels expired 24h UNPAID orders, atomically releases reserved stock, and notifies partner")
    void shouldCancelExpiredUnpaidOrdersAndReleaseStockAtomically() {
        UUID branchId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID partnerUserId = UUID.randomUUID();

        BranchEntity branch = BranchEntity.builder().id(branchId).code("HCM-01").name("HCMC Hub").build();
        ProductEntity product = ProductEntity.builder().id(productId).sku("SKU-100").name("Pro Mask").build();
        UserEntity user = UserEntity.builder().id(partnerUserId).email("partner@b2b.vn").build();

        OrderEntity expiredOrder = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderCode("WHL-EXPIRED-001")
                .user(user)
                .branch(branch)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
        expiredOrder.setCreatedAt(Instant.now().minusSeconds(90_000));

        OrderItemEntity lineItem = OrderItemEntity.builder()
                .id(1L)
                .order(expiredOrder)
                .product(product)
                .quantity(15)
                .build();
        expiredOrder.getItems().add(lineItem);

        when(orderRepository.findExpiredUnpaidOrders(any(), any())).thenReturn(List.of(expiredOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        cronScheduler.cancelExpiredUnpaidOrdersAndReleaseStock();

        assertThat(expiredOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(expiredOrder.getCancelledAt()).isNotNull();
        verify(inventoryService).releaseReservedStockForOrder(
                eq(branchId),
                org.mockito.ArgumentMatchers.anyList()
        );
        verify(notificationRepository).save(any(NotificationEntity.class));
    }
}
