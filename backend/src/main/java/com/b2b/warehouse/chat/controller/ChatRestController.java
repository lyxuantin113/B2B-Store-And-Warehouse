package com.b2b.warehouse.chat.controller;

import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.chat.dto.AssignAdminRequestDto;
import com.b2b.warehouse.chat.dto.ChatMessageDto;
import com.b2b.warehouse.chat.dto.ChatRoomDto;
import com.b2b.warehouse.chat.dto.SendMessageRequestDto;
import com.b2b.warehouse.chat.dto.UpdateRoomStatusRequestDto;
import com.b2b.warehouse.chat.entity.ChatRoomStatus;
import com.b2b.warehouse.chat.service.ChatService;
import com.b2b.warehouse.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "B2B Support Chatbox", description = "Real-time support chat between B2B Partners and Admin/Sellers (REST + WebSocket STOMP)")
public class ChatRestController {

    private final ChatService chatService;

    // =========================================================================
    // PARTNER & SHARED ENDPOINTS
    // =========================================================================

    @GetMapping("/api/v1/b2b/chat/room")
    @Operation(summary = "Get or auto-create dedicated support chat room for the authenticated B2B Partner")
    public ResponseEntity<ApiResponse<ChatRoomDto>> getMyChatRoom(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.getOrCreateRoomForPartner(currentUser.getUserId())
        ));
    }

    @GetMapping("/api/v1/b2b/chat/rooms/{roomId}/messages")
    @Operation(summary = "Get keyset-paginated chat message history for a support room")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getRoomMessages(
            @PathVariable UUID roomId,
            @RequestParam(required = false) Long cursorMessageId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.getMessageHistory(roomId, cursorMessageId, limit, currentUser)
        ));
    }

    @PostMapping("/api/v1/b2b/chat/rooms/{roomId}/read")
    @Operation(summary = "Mark all unread messages in room as read and broadcast STOMP read receipt")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markRoomAsRead(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        int markedCount = chatService.markMessagesAsRead(roomId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("markedReadCount", markedCount),
                "Messages marked as read"
        ));
    }

    @PostMapping("/api/v1/b2b/chat/messages")
    @Operation(summary = "Send a chat message via HTTP REST (fallback when WebSocket is reconnecting)")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessageRest(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody SendMessageRequestDto request
    ) {
        ChatMessageDto sent = chatService.sendMessage(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(sent, "Message sent successfully"));
    }

    // =========================================================================
    // ADMIN / SELLER CRM ENDPOINTS
    // =========================================================================

    @GetMapping("/api/v1/admin/chat/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SELLER')")
    @Operation(summary = "Admin/Seller: List all partner support chat rooms with unread counters and status filter")
    public ResponseEntity<ApiResponse<Page<ChatRoomDto>>> listRoomsForAdmin(
            @RequestParam(required = false) ChatRoomStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.listRoomsForAdmin(status, PageRequest.of(page, size))
        ));
    }

    @GetMapping("/api/v1/admin/chat/rooms/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SELLER')")
    @Operation(summary = "Admin/Seller: Get details of a specific chat room")
    public ResponseEntity<ApiResponse<ChatRoomDto>> getAdminRoomDetails(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal AuthenticatedUser adminUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.getRoomDetails(roomId, adminUser)
        ));
    }

    @PatchMapping("/api/v1/admin/chat/rooms/{roomId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Admin: Assign a seller/support agent to a partner chat room")
    public ResponseEntity<ApiResponse<ChatRoomDto>> assignAdmin(
            @PathVariable UUID roomId,
            @Valid @RequestBody AssignAdminRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.assignAdminToRoom(roomId, request.assignedAdminId()),
                "Staff member assigned to chat room"
        ));
    }

    @PatchMapping("/api/v1/admin/chat/rooms/{roomId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Admin: Open or close a partner support chat room")
    public ResponseEntity<ApiResponse<ChatRoomDto>> updateRoomStatus(
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateRoomStatusRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.updateRoomStatus(roomId, request.status()),
                "Chat room status updated"
        ));
    }
}
