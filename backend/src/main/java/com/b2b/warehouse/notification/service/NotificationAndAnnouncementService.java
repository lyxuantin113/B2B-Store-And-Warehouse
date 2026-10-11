package com.b2b.warehouse.notification.service;

import com.b2b.warehouse.common.dto.CursorPageDto;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.common.util.HtmlSanitizerUtil;
import com.b2b.warehouse.notification.dto.AnnouncementResponseDto;
import com.b2b.warehouse.notification.dto.NotificationResponseDto;
import com.b2b.warehouse.notification.dto.UnifiedNotificationCenterDto;
import com.b2b.warehouse.notification.dto.UpsertAnnouncementRequestDto;
import com.b2b.warehouse.notification.entity.AnnouncementEntity;
import com.b2b.warehouse.notification.entity.NotificationEntity;
import com.b2b.warehouse.notification.repository.AnnouncementRepository;
import com.b2b.warehouse.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages both Admin Broadcast Announcements ({@code b2b-announcements.php}) and
 * B2B Partner Personal Notifications (order status changes, payment confirmations, approval updates).
 */
@Service
@RequiredArgsConstructor
public class NotificationAndAnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final NotificationRepository notificationRepository;
    private final HtmlSanitizerUtil htmlSanitizerUtil;

    // =========================================================================
    // 1. ADMIN BROADCAST ANNOUNCEMENTS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AnnouncementResponseDto> listAllAnnouncementsForAdmin() {
        return announcementRepository.findAllByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc().stream()
                .map(this::toAnnouncementDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponseDto> listPublishedAnnouncements() {
        return announcementRepository.findAllByIsPublishedTrueAndDeletedAtIsNullOrderByIsPinnedDescPublishedAtDesc().stream()
                .map(this::toAnnouncementDto)
                .toList();
    }

    @Transactional
    public AnnouncementResponseDto createAnnouncement(UUID adminUserId, UpsertAnnouncementRequestDto request) {
        String sanitizedHtml = htmlSanitizerUtil.sanitizeRichText(request.contentHtml());
        String badge = (request.categoryBadge() == null || request.categoryBadge().isBlank())
                ? "GENERAL"
                : request.categoryBadge().trim().toUpperCase();

        AnnouncementEntity entity = AnnouncementEntity.builder()
                .title(request.title().trim())
                .contentHtml(sanitizedHtml)
                .categoryBadge(badge)
                .isPinned(Boolean.TRUE.equals(request.isPinned()))
                .isPublished(request.isPublished() == null || request.isPublished())
                .createdBy(adminUserId)
                .publishedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toAnnouncementDto(announcementRepository.save(entity));
    }

    @Transactional
    public AnnouncementResponseDto updateAnnouncement(UUID announcementId, UpsertAnnouncementRequestDto request) {
        AnnouncementEntity entity = announcementRepository.findByIdAndDeletedAtIsNull(announcementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Announcement not found"));

        entity.setTitle(request.title().trim());
        entity.setContentHtml(htmlSanitizerUtil.sanitizeRichText(request.contentHtml()));
        if (request.categoryBadge() != null && !request.categoryBadge().isBlank()) {
            entity.setCategoryBadge(request.categoryBadge().trim().toUpperCase());
        }
        if (request.isPinned() != null) {
            entity.setPinned(request.isPinned());
        }
        if (request.isPublished() != null) {
            entity.setPublished(request.isPublished());
        }
        entity.setUpdatedAt(Instant.now());

        return toAnnouncementDto(announcementRepository.save(entity));
    }

    @Transactional
    public void softDeleteAnnouncement(UUID announcementId) {
        AnnouncementEntity entity = announcementRepository.findByIdAndDeletedAtIsNull(announcementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Announcement not found"));
        entity.setDeletedAt(Instant.now());
        entity.setPublished(false);
        announcementRepository.save(entity);
    }

    // =========================================================================
    // 2. PERSONAL NOTIFICATIONS & UNIFIED FEED (B2B PARTNER TAB 5)
    // =========================================================================

    @Transactional
    public NotificationResponseDto createPersonalNotification(
            UUID userId,
            String notifType,
            String title,
            String message,
            UUID referenceId
    ) {
        NotificationEntity entity = NotificationEntity.builder()
                .userId(userId)
                .notifType(notifType)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        return toNotificationDto(notificationRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public UnifiedNotificationCenterDto getUnifiedNotificationCenter(
            UUID userId,
            boolean unreadOnly,
            Instant cursorCreatedAt,
            UUID cursorId,
            int size
    ) {
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        CursorPageDto<NotificationResponseDto> personalPage = listUserNotificationsKeyset(
                userId, unreadOnly, cursorCreatedAt, cursorId, size
        );
        List<AnnouncementResponseDto> broadcastList = listPublishedAnnouncements();

        return UnifiedNotificationCenterDto.builder()
                .unreadCount(unreadCount)
                .personalNotifications(personalPage)
                .broadcastAnnouncements(broadcastList)
                .build();
    }

    @Transactional(readOnly = true)
    public CursorPageDto<NotificationResponseDto> listUserNotificationsKeyset(
            UUID userId,
            boolean unreadOnly,
            Instant cursorCreatedAt,
            UUID cursorId,
            int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        List<NotificationEntity> fetched = notificationRepository.findUserNotificationsKeyset(
                userId,
                unreadOnly,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, safeSize + 1)
        );

        boolean hasNext = fetched.size() > safeSize;
        List<NotificationEntity> pageItems = hasNext ? fetched.subList(0, safeSize) : fetched;

        Instant nextCursorCreatedAt = null;
        UUID nextCursorId = null;
        if (hasNext && !pageItems.isEmpty()) {
            NotificationEntity last = pageItems.get(pageItems.size() - 1);
            nextCursorCreatedAt = last.getCreatedAt();
            nextCursorId = last.getId();
        }

        List<NotificationResponseDto> dtoItems = pageItems.stream()
                .map(this::toNotificationDto)
                .toList();

        return new CursorPageDto<>(dtoItems, nextCursorCreatedAt, nextCursorId, hasNext);
    }

    @Transactional
    public NotificationResponseDto markNotificationAsRead(UUID userId, UUID notificationId) {
        NotificationEntity entity = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found"));

        if (!entity.isRead()) {
            entity.setRead(true);
            entity.setReadAt(Instant.now());
            entity = notificationRepository.save(entity);
        }
        return toNotificationDto(entity);
    }

    @Transactional
    public int markAllNotificationsAsRead(UUID userId) {
        return notificationRepository.markAllAsReadForUser(userId, Instant.now());
    }

    private AnnouncementResponseDto toAnnouncementDto(AnnouncementEntity entity) {
        return AnnouncementResponseDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .contentHtml(entity.getContentHtml())
                .categoryBadge(entity.getCategoryBadge())
                .isPinned(entity.isPinned())
                .isPublished(entity.isPublished())
                .createdBy(entity.getCreatedBy())
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private NotificationResponseDto toNotificationDto(NotificationEntity entity) {
        return NotificationResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .notifType(entity.getNotifType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .referenceId(entity.getReferenceId())
                .isRead(entity.isRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
