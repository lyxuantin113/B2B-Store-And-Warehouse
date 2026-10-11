package com.b2b.warehouse.async;

import com.b2b.warehouse.notification.service.NotificationAndAnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Translates domain events (from Outbox/Kafka) into user-facing notifications
 * in the {@code notifications} table for the B2B Partner Portal Notification Center (Tab 5).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventNotificationProcessor {

    private final NotificationAndAnnouncementService notificationService;

    @Transactional
    public void processEvent(String eventType, String aggregateId, Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("userId")) {
            return;
        }

        UUID userId = parseUuidSafely(payload.get("userId"));
        if (userId == null) {
            return;
        }
        UUID referenceId = parseUuidSafely(aggregateId);
        String orderCode = String.valueOf(payload.getOrDefault("orderCode", aggregateId));

        switch (eventType) {
            case "ORDER_CREATED" -> notificationService.createPersonalNotification(
                    userId,
                    "ORDER_STATUS",
                    "Order Created: " + orderCode,
                    "Your B2B order " + orderCode + " has been placed and stock has been reserved.",
                    referenceId
            );
            case "ORDER_PAYMENT_CONFIRMED" -> notificationService.createPersonalNotification(
                    userId,
                    "PAYMENT_CONFIRMED",
                    "Payment Confirmed: " + orderCode,
                    "Payment for order " + orderCode + " was verified via " + payload.getOrDefault("provider", "Gateway") + ".",
                    referenceId
            );
            case "ORDER_STATUS_CHANGED" -> notificationService.createPersonalNotification(
                    userId,
                    "ORDER_STATUS",
                    "Order Status Updated: " + orderCode,
                    "Order " + orderCode + " transitioned from "
                            + payload.getOrDefault("previousStatus", "PENDING")
                            + " to " + payload.getOrDefault("newStatus", "UPDATED") + ".",
                    referenceId
            );
            case "ORDER_WAYBILL_ASSIGNED" -> notificationService.createPersonalNotification(
                    userId,
                    "ORDER_SHIPPED",
                    "Order Shipped: " + orderCode,
                    "Your order " + orderCode + " has been dispatched with tracking number "
                            + payload.getOrDefault("trackingNumber", "N/A") + ".",
                    referenceId
            );
            default -> log.debug("Unhandled domain event type for user notification: {}", eventType);
        }
    }

    private UUID parseUuidSafely(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
