package com.b2b.warehouse.chat.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ChatReadReceiptEvent(
        UUID roomId,
        UUID readerUserId,
        int markedCount,
        Instant readAt
) {
}
