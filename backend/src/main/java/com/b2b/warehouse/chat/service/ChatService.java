package com.b2b.warehouse.chat.service;

import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.auth.repository.UserRepository;
import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.chat.dto.ChatMessageDto;
import com.b2b.warehouse.chat.dto.ChatReadReceiptEvent;
import com.b2b.warehouse.chat.dto.ChatRoomDto;
import com.b2b.warehouse.chat.dto.SendMessageRequestDto;
import com.b2b.warehouse.chat.entity.ChatMessageEntity;
import com.b2b.warehouse.chat.entity.ChatMessageType;
import com.b2b.warehouse.chat.entity.ChatRoomEntity;
import com.b2b.warehouse.chat.entity.ChatRoomStatus;
import com.b2b.warehouse.chat.repository.ChatMessageRepository;
import com.b2b.warehouse.chat.repository.ChatRoomRepository;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.common.util.HtmlSanitizerUtil;
import com.b2b.warehouse.partner.entity.B2bPartnerProfileEntity;
import com.b2b.warehouse.partner.repository.B2bPartnerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing real-time support chat rooms, messages, read receipts,
 * and STOMP message broadcasting for B2B Partners and Admin/Sellers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final B2bPartnerProfileRepository partnerProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final HtmlSanitizerUtil htmlSanitizerUtil;

    // =========================================================================
    // 1. CHAT ROOM LIFECYCLE & DISCOVERY
    // =========================================================================

    /**
     * Gets or automatically creates the dedicated support chat room for the authenticated B2B Partner.
     */
    @Transactional
    public ChatRoomDto getOrCreateRoomForPartner(UUID partnerUserId) {
        Optional<ChatRoomEntity> existing = chatRoomRepository.findByPartnerUser_Id(partnerUserId);
        if (existing.isPresent()) {
            return toRoomDto(existing.get(), partnerUserId);
        }

        UserEntity partnerUser = userRepository.findById(partnerUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        ChatRoomEntity newRoom = ChatRoomEntity.builder()
                .partnerUser(partnerUser)
                .status(ChatRoomStatus.OPEN)
                .lastMessageAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        ChatRoomEntity saved = chatRoomRepository.save(newRoom);
        log.info("Created support chat room {} for partner {}", saved.getId(), partnerUserId);
        return toRoomDto(saved, partnerUserId);
    }

    @Transactional(readOnly = true)
    public ChatRoomDto getRoomDetails(UUID roomId, AuthenticatedUser currentUser) {
        ChatRoomEntity room = chatRoomRepository.findWithDetailsById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chat room not found"));

        enforceRoomAccess(room, currentUser);
        return toRoomDto(room, currentUser.getUserId());
    }

    @Transactional(readOnly = true)
    public Page<ChatRoomDto> listRoomsForAdmin(ChatRoomStatus status, Pageable pageable) {
        Page<ChatRoomEntity> roomsPage = chatRoomRepository.findRoomsForAdmin(status, pageable);
        return roomsPage.map(room -> toRoomDto(room, null));
    }

    // =========================================================================
    // 2. MESSAGING & REAL-TIME STOMP BROADCASTING
    // =========================================================================

    @Transactional
    public ChatMessageDto sendMessage(AuthenticatedUser sender, SendMessageRequestDto request) {
        ChatRoomEntity room;
        if (request.roomId() != null) {
            room = chatRoomRepository.findWithDetailsById(request.roomId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chat room not found"));
            enforceRoomAccess(room, sender);
        } else if (isPartner(sender)) {
            room = chatRoomRepository.findByPartnerUser_Id(sender.getUserId())
                    .orElseGet(() -> {
                        UserEntity partner = userRepository.findById(sender.getUserId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
                        return chatRoomRepository.save(ChatRoomEntity.builder()
                                .partnerUser(partner)
                                .status(ChatRoomStatus.OPEN)
                                .lastMessageAt(Instant.now())
                                .createdAt(Instant.now())
                                .build());
                    });
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "roomId is required for staff members");
        }

        // Reopen room if closed when partner sends a new message
        if (room.getStatus() == ChatRoomStatus.CLOSED && isPartner(sender)) {
            room.setStatus(ChatRoomStatus.OPEN);
        }

        UserEntity senderEntity = userRepository.findById(sender.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Sender user not found"));

        // Sanitize content against XSS if text message
        String cleanContent = request.content().trim();
        if (request.resolvedMessageType() == ChatMessageType.TEXT) {
            cleanContent = htmlSanitizerUtil.sanitizeRichText(cleanContent);
            if (cleanContent.isBlank()) {
                cleanContent = request.content().trim(); // Fallback if plain text contains no tags
            }
        }

        ChatMessageEntity messageEntity = ChatMessageEntity.builder()
                .room(room)
                .sender(senderEntity)
                .messageType(request.resolvedMessageType())
                .content(cleanContent)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        ChatMessageEntity saved = chatMessageRepository.save(messageEntity);
        room.setLastMessageAt(saved.getCreatedAt());
        chatRoomRepository.save(room);

        ChatMessageDto dto = toMessageDto(saved);

        // Real-time broadcast to room subscribers
        String roomDestination = "/topic/chat.room." + room.getId();
        messagingTemplate.convertAndSend(roomDestination, dto);

        // Real-time notification toast for Admin/Seller if message is from Partner
        if (isPartner(sender)) {
            messagingTemplate.convertAndSend("/topic/admin.chat.notifications", dto);
        }

        log.debug("Sent chat message {} in room {} by user {}", saved.getId(), room.getId(), sender.getUserId());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessageHistory(
            UUID roomId,
            Long cursorMessageId,
            int limit,
            AuthenticatedUser currentUser
    ) {
        ChatRoomEntity room = chatRoomRepository.findWithDetailsById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chat room not found"));
        enforceRoomAccess(room, currentUser);

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<ChatMessageEntity> messagesDesc = chatMessageRepository.findMessagesKeyset(
                roomId,
                cursorMessageId,
                PageRequest.of(0, safeLimit)
        );

        // Reverse to chronological order (oldest to newest) for client display
        Collections.reverse(messagesDesc);
        return messagesDesc.stream()
                .map(this::toMessageDto)
                .toList();
    }

    @Transactional
    public int markMessagesAsRead(UUID roomId, AuthenticatedUser currentUser) {
        ChatRoomEntity room = chatRoomRepository.findWithDetailsById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chat room not found"));
        enforceRoomAccess(room, currentUser);

        int markedCount = chatMessageRepository.markRoomMessagesAsRead(roomId, currentUser.getUserId());
        if (markedCount > 0) {
            ChatReadReceiptEvent receiptEvent = ChatReadReceiptEvent.builder()
                    .roomId(roomId)
                    .readerUserId(currentUser.getUserId())
                    .markedCount(markedCount)
                    .readAt(Instant.now())
                    .build();
            messagingTemplate.convertAndSend("/topic/chat.room." + roomId + ".read", receiptEvent);
        }
        return markedCount;
    }

    // =========================================================================
    // 3. ADMIN ROOM MANAGEMENT (ASSIGNMENT & STATUS)
    // =========================================================================

    @Transactional
    public ChatRoomDto assignAdminToRoom(UUID roomId, UUID adminUserId) {
        ChatRoomEntity room = chatRoomRepository.findWithDetailsById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chat room not found"));

        UserEntity admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Admin user not found"));

        room.setAssignedAdmin(admin);
        ChatRoomEntity saved = chatRoomRepository.save(room);
        log.info("Assigned admin {} to chat room {}", adminUserId, roomId);
        return toRoomDto(saved, null);
    }

    @Transactional
    public ChatRoomDto updateRoomStatus(UUID roomId, ChatRoomStatus status) {
        ChatRoomEntity room = chatRoomRepository.findWithDetailsById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chat room not found"));

        room.setStatus(status);
        ChatRoomEntity saved = chatRoomRepository.save(room);
        log.info("Updated chat room {} status to {}", roomId, status);
        return toRoomDto(saved, null);
    }

    // =========================================================================
    // 4. HELPER METHODS & MAPPING
    // =========================================================================

    public void enforceRoomAccess(ChatRoomEntity room, AuthenticatedUser user) {
        if (!isStaff(user) && !room.getPartnerUser().getId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied to chat room " + room.getId());
        }
    }

    public boolean isStaff(AuthenticatedUser user) {
        if (user == null || user.getRoleCode() == null) {
            return false;
        }
        String role = user.getRoleCode().toUpperCase();
        return role.equals("ADMIN") || role.equals("MANAGER") || role.equals("SELLER");
    }

    public boolean isPartner(AuthenticatedUser user) {
        return !isStaff(user);
    }

    private ChatRoomDto toRoomDto(ChatRoomEntity room, UUID forUserId) {
        UUID partnerId = room.getPartnerUser().getId();
        String partnerCompanyName = null;
        Optional<B2bPartnerProfileEntity> profile = partnerProfileRepository.findByUserId(partnerId);
        if (profile.isPresent()) {
            partnerCompanyName = profile.get().getCompanyName();
        }

        long unreadCount = forUserId != null
                ? chatRoomRepository.countUnreadMessagesForUser(room.getId(), forUserId)
                : 0L;

        String preview = null;
        Optional<ChatMessageEntity> lastMsg = chatMessageRepository.findTop1ByRoom_IdOrderByIdDesc(room.getId());
        if (lastMsg.isPresent()) {
            preview = lastMsg.get().getContent();
            if (preview != null && preview.length() > 60) {
                preview = preview.substring(0, 57) + "...";
            }
        }

        return ChatRoomDto.builder()
                .id(room.getId())
                .partnerUserId(partnerId)
                .partnerName(room.getPartnerUser().getFullName())
                .partnerEmail(room.getPartnerUser().getEmail())
                .partnerCompanyName(partnerCompanyName)
                .assignedAdminId(room.getAssignedAdmin() != null ? room.getAssignedAdmin().getId() : null)
                .assignedAdminName(room.getAssignedAdmin() != null ? room.getAssignedAdmin().getFullName() : null)
                .status(room.getStatus())
                .lastMessageAt(room.getLastMessageAt())
                .createdAt(room.getCreatedAt())
                .unreadCount(unreadCount)
                .lastMessagePreview(preview)
                .build();
    }

    private ChatMessageDto toMessageDto(ChatMessageEntity message) {
        String roleCode = message.getSender().getRole() != null
                ? message.getSender().getRole().getCode()
                : "USER";

        return ChatMessageDto.builder()
                .id(message.getId())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .senderRole(roleCode)
                .messageType(message.getMessageType())
                .content(message.getContent())
                .isRead(message.isRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
