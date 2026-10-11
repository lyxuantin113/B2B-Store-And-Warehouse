package com.b2b.warehouse.notification.controller;

import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.notification.dto.AnnouncementResponseDto;
import com.b2b.warehouse.notification.dto.NotificationResponseDto;
import com.b2b.warehouse.notification.dto.UnifiedNotificationCenterDto;
import com.b2b.warehouse.notification.service.NotificationAndAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "B2B Unified Notification Center", description = "Order status alerts, unread badge counter, and Admin Broadcast Announcements")
public class B2bNotificationController {

    private final NotificationAndAnnouncementService notificationAndAnnouncementService;

    @GetMapping("/api/v1/public/announcements")
    @Operation(summary = "List active broadcast announcements (pinned first)")
    public ResponseEntity<ApiResponse<List<AnnouncementResponseDto>>> listPublicAnnouncements() {
        return ResponseEntity.ok(ApiResponse.ok(notificationAndAnnouncementService.listPublishedAnnouncements()));
    }

    @GetMapping("/api/v1/b2b/notifications")
    @Operation(summary = "Get B2B Partner Unified Notification Center (unread count + personal order alerts + broadcast announcements)")
    public ResponseEntity<ApiResponse<UnifiedNotificationCenterDto>> getUnifiedNotificationCenter(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Instant cursorCreatedAt,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "20") int size
    ) {
        UnifiedNotificationCenterDto feed = notificationAndAnnouncementService.getUnifiedNotificationCenter(
                currentUser.getUserId(), unreadOnly, cursorCreatedAt, cursorId, size
        );
        return ResponseEntity.ok(ApiResponse.ok(feed));
    }

    @PatchMapping("/api/v1/b2b/notifications/{notificationId}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> markAsRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID notificationId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationAndAnnouncementService.markNotificationAsRead(currentUser.getUserId(), notificationId)
        ));
    }

    @PatchMapping("/api/v1/b2b/notifications/read-all")
    @Operation(summary = "Mark all unread notifications as read in a single atomic SQL UPDATE")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        int updatedCount = notificationAndAnnouncementService.markAllNotificationsAsRead(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("markedReadCount", updatedCount),
                "All notifications marked as read"
        ));
    }
}
