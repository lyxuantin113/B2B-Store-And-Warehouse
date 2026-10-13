package com.b2b.warehouse.chat.dto;

import com.b2b.warehouse.chat.entity.ChatMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequestDto(
        UUID roomId,
        ChatMessageType messageType,

        @NotBlank(message = "Message content must not be blank")
        @Size(max = 4000, message = "Message content must not exceed 4000 characters")
        String content
) {
    public ChatMessageType resolvedMessageType() {
        return messageType != null ? messageType : ChatMessageType.TEXT;
    }
}
