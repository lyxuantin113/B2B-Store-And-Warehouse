package com.b2b.warehouse.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertAnnouncementRequestDto(
        @NotBlank(message = "Announcement title is required")
        @Size(max = 200)
        String title,

        @NotBlank(message = "Announcement HTML content is required")
        String contentHtml,

        @Size(max = 40)
        String categoryBadge,

        Boolean isPinned,

        Boolean isPublished
) {
}
