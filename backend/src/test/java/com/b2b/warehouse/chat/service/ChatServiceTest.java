package com.b2b.warehouse.chat.service;

import com.b2b.warehouse.auth.entity.AccountStatus;
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
import com.b2b.warehouse.rbac.entity.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private B2bPartnerProfileRepository partnerProfileRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private HtmlSanitizerUtil htmlSanitizerUtil;
    private ChatService chatService;

    private UUID partnerUserId;
    private UUID otherPartnerId;
    private UUID adminUserId;
    private UserEntity partnerEntity;
    private UserEntity adminEntity;
    private AuthenticatedUser partnerUser;
    private AuthenticatedUser adminUser;
    private AuthenticatedUser otherPartnerUser;
    private ChatRoomEntity roomEntity;

    @BeforeEach
    void setUp() {
        htmlSanitizerUtil = new HtmlSanitizerUtil();
        chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                userRepository,
                partnerProfileRepository,
                messagingTemplate,
                htmlSanitizerUtil
        );

        partnerUserId = UUID.randomUUID();
        otherPartnerId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();

        RoleEntity partnerRole = RoleEntity.builder().code("B2B_PARTNER").name("Partner").build();
        RoleEntity adminRole = RoleEntity.builder().code("ADMIN").name("Admin").build();

        partnerEntity = UserEntity.builder()
                .id(partnerUserId)
                .email("partner@b2b.com")
                .fullName("Partner One")
                .role(partnerRole)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        adminEntity = UserEntity.builder()
                .id(adminUserId)
                .email("admin@warehouse.com")
                .fullName("Admin Master")
                .role(adminRole)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        partnerUser = AuthenticatedUser.builder()
                .userId(partnerUserId)
                .email("partner@b2b.com")
                .roleCode("B2B_PARTNER")
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(1)
                .permissions(List.of("order:read", "order:write"))
                .build();

        otherPartnerUser = AuthenticatedUser.builder()
                .userId(otherPartnerId)
                .email("other@b2b.com")
                .roleCode("B2B_PARTNER")
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(1)
                .permissions(List.of("order:read", "order:write"))
                .build();

        adminUser = AuthenticatedUser.builder()
                .userId(adminUserId)
                .email("admin@warehouse.com")
                .roleCode("ADMIN")
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(1)
                .permissions(List.of("order:manage", "partner:read"))
                .build();

        roomEntity = ChatRoomEntity.builder()
                .id(UUID.randomUUID())
                .partnerUser(partnerEntity)
                .status(ChatRoomStatus.OPEN)
                .lastMessageAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getOrCreateRoomForPartner creates a new room if none exists and attaches partner profile")
    void shouldCreateRoomIfNoneExists() {
        when(chatRoomRepository.findByPartnerUser_Id(partnerUserId)).thenReturn(Optional.empty());
        when(userRepository.findById(partnerUserId)).thenReturn(Optional.of(partnerEntity));
        when(chatRoomRepository.save(any(ChatRoomEntity.class))).thenReturn(roomEntity);
        when(partnerProfileRepository.findByUserId(partnerUserId)).thenReturn(Optional.of(
                B2bPartnerProfileEntity.builder()
                        .companyName("VinCommerce Partner Co")
                        .build()
        ));

        ChatRoomDto result = chatService.getOrCreateRoomForPartner(partnerUserId);

        assertThat(result).isNotNull();
        assertThat(result.partnerUserId()).isEqualTo(partnerUserId);
        assertThat(result.partnerCompanyName()).isEqualTo("VinCommerce Partner Co");
        verify(chatRoomRepository).save(any(ChatRoomEntity.class));
    }

    @Test
    @DisplayName("sendMessage sanitizes HTML XSS, updates room timestamp, and broadcasts via STOMP")
    void shouldSendMessageAndBroadcastViaStomp() {
        when(chatRoomRepository.findWithDetailsById(roomEntity.getId())).thenReturn(Optional.of(roomEntity));
        when(userRepository.findById(partnerUserId)).thenReturn(Optional.of(partnerEntity));
        when(chatRoomRepository.save(any(ChatRoomEntity.class))).thenReturn(roomEntity);

        String maliciousContent = "Hello seller! <script>alert('xss')</script>Can we check order #UM-1234?";
        SendMessageRequestDto request = new SendMessageRequestDto(
                roomEntity.getId(),
                ChatMessageType.TEXT,
                maliciousContent
        );

        when(chatMessageRepository.save(any(ChatMessageEntity.class))).thenAnswer(inv -> {
            ChatMessageEntity entity = inv.getArgument(0);
            entity.setId(101L);
            return entity;
        });

        ChatMessageDto result = chatService.sendMessage(partnerUser, request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.content()).doesNotContain("<script>");
        assertThat(result.content()).contains("Hello seller!");

        // Verify STOMP broadcast to room and admin notification channel
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.room." + roomEntity.getId()), any(ChatMessageDto.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/admin.chat.notifications"), any(ChatMessageDto.class));
    }

    @Test
    @DisplayName("enforceRoomAccess throws FORBIDDEN when a partner tries to access another partner's room")
    void shouldDenyAccessToOtherPartnerRoom() {
        when(chatRoomRepository.findWithDetailsById(roomEntity.getId())).thenReturn(Optional.of(roomEntity));

        SendMessageRequestDto request = new SendMessageRequestDto(
                roomEntity.getId(),
                ChatMessageType.TEXT,
                "Unauthorized intrusion message"
        );

        assertThatThrownBy(() -> chatService.sendMessage(otherPartnerUser, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("markMessagesAsRead updates unread messages and broadcasts ChatReadReceiptEvent")
    void shouldMarkMessagesReadAndBroadcastReceipt() {
        when(chatRoomRepository.findWithDetailsById(roomEntity.getId())).thenReturn(Optional.of(roomEntity));
        when(chatMessageRepository.markRoomMessagesAsRead(roomEntity.getId(), adminUserId)).thenReturn(3);

        int count = chatService.markMessagesAsRead(roomEntity.getId(), adminUser);

        assertThat(count).isEqualTo(3);
        ArgumentCaptor<ChatReadReceiptEvent> captor = ArgumentCaptor.forClass(ChatReadReceiptEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.room." + roomEntity.getId() + ".read"), captor.capture());
        assertThat(captor.getValue().markedCount()).isEqualTo(3);
        assertThat(captor.getValue().readerUserId()).isEqualTo(adminUserId);
    }

    @Test
    @DisplayName("assignAdminToRoom and updateRoomStatus update room metadata successfully")
    void shouldAssignAdminAndCloseRoom() {
        when(chatRoomRepository.findWithDetailsById(roomEntity.getId())).thenReturn(Optional.of(roomEntity));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminEntity));
        when(chatRoomRepository.save(any(ChatRoomEntity.class))).thenReturn(roomEntity);

        ChatRoomDto assigned = chatService.assignAdminToRoom(roomEntity.getId(), adminUserId);
        assertThat(assigned).isNotNull();

        when(chatRoomRepository.findWithDetailsById(roomEntity.getId())).thenReturn(Optional.of(roomEntity));
        ChatRoomDto closed = chatService.updateRoomStatus(roomEntity.getId(), ChatRoomStatus.CLOSED);
        assertThat(closed).isNotNull();
        assertThat(roomEntity.getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
    }
}
