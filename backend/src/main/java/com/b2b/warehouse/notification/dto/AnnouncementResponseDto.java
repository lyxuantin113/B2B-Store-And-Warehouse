package com.b2b.warehouse.notification.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AnnouncementResponseDto(
        UUID id,
        String title,
        String contentHtml,
        String categoryBadge,
        boolean isPinned,
        boolean isPublished,
        UUID createdBy,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
