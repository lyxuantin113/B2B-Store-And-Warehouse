package com.b2b.warehouse.chat.dto;

import com.b2b.warehouse.chat.entity.ChatRoomStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ChatRoomDto(
        UUID id,
        UUID partnerUserId,
        String partnerName,
        String partnerEmail,
        String partnerCompanyName,
        UUID assignedAdminId,
        String assignedAdminName,
        ChatRoomStatus status,
        Instant lastMessageAt,
        Instant createdAt,
        long unreadCount,
        String lastMessagePreview
) {
}
