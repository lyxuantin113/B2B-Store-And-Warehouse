package com.b2b.warehouse.notification.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationResponseDto(
        UUID id,
        UUID userId,
        String notifType,
        String title,
        String message,
        UUID referenceId,
        boolean isRead,
        Instant readAt,
        Instant createdAt
) {
}
