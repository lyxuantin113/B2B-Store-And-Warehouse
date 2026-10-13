package com.b2b.warehouse.chat.controller;

import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.chat.dto.ChatMessageDto;
import com.b2b.warehouse.chat.dto.SendMessageRequestDto;
import com.b2b.warehouse.chat.service.ChatService;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP WebSocket Message Controller handling real-time {@code /app/chat.send} and {@code /app/chat.read}.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void handleChatMessage(
            @Valid @Payload SendMessageRequestDto request,
            Principal principal
    ) {
        AuthenticatedUser user = extractAuthenticatedUser(principal);
        log.debug("STOMP /app/chat.send received from user={}", user.getUserId());
        chatService.sendMessage(user, request);
    }

    @MessageMapping("/chat.read")
    public void handleMarkRead(
            @Payload Map<String, Object> payload,
            Principal principal
    ) {
        AuthenticatedUser user = extractAuthenticatedUser(principal);
        Object rawRoomId = payload.get("roomId");
        if (rawRoomId == null) {
            log.warn("STOMP /app/chat.read missing roomId parameter");
            return;
        }

        try {
            UUID roomId = UUID.fromString(String.valueOf(rawRoomId));
            chatService.markMessagesAsRead(roomId, user);
        } catch (IllegalArgumentException ex) {
            log.warn("STOMP /app/chat.read received invalid roomId: {}", rawRoomId);
        }
    }

    private AuthenticatedUser extractAuthenticatedUser(Principal principal) {
        if (principal instanceof Authentication auth
                && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "Unauthenticated WebSocket message transmission");
    }
}
