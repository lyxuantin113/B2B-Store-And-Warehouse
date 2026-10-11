package com.b2b.warehouse.notification.controller;

import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.notification.dto.AnnouncementResponseDto;
import com.b2b.warehouse.notification.dto.UpsertAnnouncementRequestDto;
import com.b2b.warehouse.notification.service.NotificationAndAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/announcements")
@RequiredArgsConstructor
@Tag(name = "Admin Broadcast Announcements", description = "Create, pin, publish, and soft-delete B2B broadcast notices (sanitized via OWASP HTML Sanitizer)")
public class AdminAnnouncementController {

    private final NotificationAndAnnouncementService notificationAndAnnouncementService;

    @GetMapping
    @PreAuthorize("hasAuthority('announcement:write')")
    @Operation(summary = "List all broadcast announcements for Admin")
    public ResponseEntity<ApiResponse<List<AnnouncementResponseDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(notificationAndAnnouncementService.listAllAnnouncementsForAdmin()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('announcement:write')")
    @Operation(summary = "Create and publish a new broadcast announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponseDto>> create(
            @AuthenticationPrincipal AuthenticatedUser adminUser,
            @Valid @RequestBody UpsertAnnouncementRequestDto request
    ) {
        AnnouncementResponseDto created = notificationAndAnnouncementService.createAnnouncement(
                adminUser.getUserId(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Broadcast announcement published"));
    }

    @PutMapping("/{announcementId}")
    @PreAuthorize("hasAuthority('announcement:write')")
    @Operation(summary = "Update an existing broadcast announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponseDto>> update(
            @PathVariable UUID announcementId,
            @Valid @RequestBody UpsertAnnouncementRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationAndAnnouncementService.updateAnnouncement(announcementId, request),
                "Broadcast announcement updated"
        ));
    }

    @DeleteMapping("/{announcementId}")
    @PreAuthorize("hasAuthority('announcement:write')")
    @Operation(summary = "Soft-delete a broadcast announcement")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID announcementId) {
        notificationAndAnnouncementService.softDeleteAnnouncement(announcementId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Broadcast announcement deleted"));
    }
}
