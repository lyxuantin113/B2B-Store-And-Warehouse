package com.b2b.warehouse.chat.config;

import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.auth.security.JwtTokenProvider;
import com.b2b.warehouse.chat.entity.ChatRoomEntity;
import com.b2b.warehouse.chat.repository.ChatRoomRepository;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketAuthChannelInterceptor interceptor;

    private UUID partnerId;
    private UUID otherPartnerId;
    private UUID adminId;
    private AuthenticatedUser partnerUser;
    private AuthenticatedUser otherPartnerUser;
    private AuthenticatedUser adminUser;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthChannelInterceptor(jwtTokenProvider, chatRoomRepository);

        partnerId = UUID.randomUUID();
        otherPartnerId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        partnerUser = AuthenticatedUser.builder()
                .userId(partnerId)
                .email("partner@b2b.com")
                .roleCode("B2B_PARTNER")
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(1)
                .permissions(List.of("order:read"))
                .build();

        otherPartnerUser = AuthenticatedUser.builder()
                .userId(otherPartnerId)
                .email("other@b2b.com")
                .roleCode("B2B_PARTNER")
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(1)
                .permissions(List.of("order:read"))
                .build();

        adminUser = AuthenticatedUser.builder()
                .userId(adminId)
                .email("admin@warehouse.com")
                .roleCode("ADMIN")
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(1)
                .permissions(List.of("order:manage"))
                .build();
    }

    @Test
    @DisplayName("STOMP CONNECT with valid Bearer token sets AuthenticatedUser on StompHeaderAccessor")
    void shouldAuthenticateStompConnectWithValidToken() {
        String token = "valid.jwt.token";
        when(jwtTokenProvider.extractAuthenticatedUser(token)).thenReturn(partnerUser);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> processed = interceptor.preSend(message, messageChannel);

        StompHeaderAccessor processedAccessor = StompHeaderAccessor.wrap(processed);
        assertThat(processedAccessor.getUser()).isNotNull();
        Authentication auth = (Authentication) processedAccessor.getUser();
        assertThat(auth.getPrincipal()).isEqualTo(partnerUser);
    }

    @Test
    @DisplayName("STOMP CONNECT without Authorization header throws UNAUTHORIZED")
    void shouldRejectStompConnectWithoutToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE to /topic/admin.chat.notifications is allowed for staff but forbidden for partner")
    void shouldEnforceAdminTopicAccessControl() {
        // Staff allowed
        StompHeaderAccessor staffAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        staffAccessor.setDestination("/topic/admin.chat.notifications");
        staffAccessor.setUser(new UsernamePasswordAuthenticationToken(adminUser, null));
        Message<?> staffMessage = MessageBuilder.createMessage(new byte[0], staffAccessor.getMessageHeaders());
        assertThat(interceptor.preSend(staffMessage, messageChannel)).isNotNull();

        // Partner rejected
        StompHeaderAccessor partnerAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        partnerAccessor.setDestination("/topic/admin.chat.notifications");
        partnerAccessor.setUser(new UsernamePasswordAuthenticationToken(partnerUser, null));
        Message<?> partnerMessage = MessageBuilder.createMessage(new byte[0], partnerAccessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(partnerMessage, messageChannel))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE to /topic/chat.room.{id} allows room owner but denies other partner")
    void shouldIsolatePartnerRoomSubscriptions() {
        UUID roomId = UUID.randomUUID();
        UserEntity partnerOwner = UserEntity.builder().id(partnerId).build();
        ChatRoomEntity room = ChatRoomEntity.builder().id(roomId).partnerUser(partnerOwner).build();

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        // Room owner allowed
        StompHeaderAccessor ownerAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        ownerAccessor.setDestination("/topic/chat.room." + roomId);
        ownerAccessor.setUser(new UsernamePasswordAuthenticationToken(partnerUser, null));
        Message<?> ownerMessage = MessageBuilder.createMessage(new byte[0], ownerAccessor.getMessageHeaders());
        assertThat(interceptor.preSend(ownerMessage, messageChannel)).isNotNull();

        // Other partner denied
        StompHeaderAccessor otherAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        otherAccessor.setDestination("/topic/chat.room." + roomId);
        otherAccessor.setUser(new UsernamePasswordAuthenticationToken(otherPartnerUser, null));
        Message<?> otherMessage = MessageBuilder.createMessage(new byte[0], otherAccessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(otherMessage, messageChannel))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
