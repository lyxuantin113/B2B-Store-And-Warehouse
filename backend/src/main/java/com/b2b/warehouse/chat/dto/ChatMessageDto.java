package com.b2b.warehouse.chat.dto;

import com.b2b.warehouse.chat.entity.ChatMessageType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ChatMessageDto(
        Long id,
        UUID roomId,
        UUID senderId,
        String senderName,
        String senderRole,
        ChatMessageType messageType,
        String content,
        boolean isRead,
        Instant createdAt
) {
}
