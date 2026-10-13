package com.b2b.warehouse.chat.dto;

import com.b2b.warehouse.chat.entity.ChatRoomStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRoomStatusRequestDto(
        @NotNull(message = "status is required (OPEN or CLOSED)")
        ChatRoomStatus status
) {
}
