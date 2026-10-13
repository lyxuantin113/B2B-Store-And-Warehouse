package com.b2b.warehouse.chat.config;

import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.auth.security.JwtTokenProvider;
import com.b2b.warehouse.chat.entity.ChatRoomEntity;
import com.b2b.warehouse.chat.repository.ChatRoomRepository;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * ChannelInterceptor enforcing JWT Dual-Token authentication during STOMP {@code CONNECT}
 * and authorization/isolation during STOMP {@code SUBSCRIBE} to support chat topics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            handleStompConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleStompSubscribe(accessor);
        }

        return message;
    }

    private void handleStompConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            authHeader = accessor.getFirstNativeHeader("X-Authorization");
        }
        if (authHeader == null || authHeader.isBlank()) {
            authHeader = accessor.getPasscode();
        }

        if (authHeader == null || authHeader.isBlank()) {
            log.warn("STOMP CONNECT rejected: Missing authorization header");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing authorization token in STOMP connect");
        }

        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
        try {
            AuthenticatedUser user = jwtTokenProvider.extractAuthenticatedUser(token);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user.getAuthorities()
            );
            accessor.setUser(authentication);
            log.debug("STOMP CONNECT authenticated for user={}", user.getUserId());
        } catch (BusinessException bex) {
            throw bex;
        } catch (Exception ex) {
            log.warn("STOMP CONNECT rejected: Invalid JWT token - {}", ex.getMessage());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid authentication token in STOMP connect");
        }
    }

    private void handleStompSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Authentication auth = (Authentication) accessor.getUser();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            log.warn("STOMP SUBSCRIBE rejected on {}: Unauthenticated user", destination);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Unauthenticated user cannot subscribe to topics");
        }

        boolean isStaff = isStaffUser(user);

        // Access control for Admin broadcast notifications
        if (destination.startsWith("/topic/admin.")) {
            if (!isStaff) {
                log.warn("STOMP SUBSCRIBE forbidden on {}: User {} is not staff", destination, user.getUserId());
                throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied to admin chat topic");
            }
            return;
        }

        // Access control for specific chat room topic: /topic/chat.room.{roomId} or /topic/chat.room.{roomId}.read
        if (destination.startsWith("/topic/chat.room.")) {
            String topicSuffix = destination.substring("/topic/chat.room.".length());
            String roomIdStr = topicSuffix.endsWith(".read")
                    ? topicSuffix.substring(0, topicSuffix.length() - ".read".length())
                    : topicSuffix;

            try {
                UUID roomId = UUID.fromString(roomIdStr);
                if (!isStaff) {
                    Optional<ChatRoomEntity> roomOpt = chatRoomRepository.findById(roomId);
                    if (roomOpt.isEmpty() || !roomOpt.get().getPartnerUser().getId().equals(user.getUserId())) {
                        log.warn("STOMP SUBSCRIBE forbidden on {}: Partner {} does not own room {}",
                                destination, user.getUserId(), roomId);
                        throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied to chat room " + roomId);
                    }
                }
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid room ID in destination: {}", destination);
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid chat room topic destination");
            }
        }
    }

    private boolean isStaffUser(AuthenticatedUser user) {
        if (user == null || user.getRoleCode() == null) {
            return false;
        }
        String role = user.getRoleCode().toUpperCase();
        return role.equals("ADMIN") || role.equals("MANAGER") || role.equals("SELLER");
    }
}
