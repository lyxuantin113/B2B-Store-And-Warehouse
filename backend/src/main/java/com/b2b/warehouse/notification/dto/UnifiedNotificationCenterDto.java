package com.b2b.warehouse.notification.dto;

import com.b2b.warehouse.common.dto.CursorPageDto;
import lombok.Builder;

import java.util.List;

@Builder
public record UnifiedNotificationCenterDto(
        long unreadCount,
        CursorPageDto<NotificationResponseDto> personalNotifications,
        List<AnnouncementResponseDto> broadcastAnnouncements
) {
}
