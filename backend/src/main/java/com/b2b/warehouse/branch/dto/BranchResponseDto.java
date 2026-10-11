package com.b2b.warehouse.branch.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record BranchResponseDto(
        UUID id,
        String code,
        String name,
        String categoryTag,
        String phone,
        String postalCode,
        String addressLine,
        String addressDetail,
        Double latitude,
        Double longitude,
        boolean isPinned,
        boolean isActive,
        String operatingHours,
        Instant createdAt,
        Instant updatedAt
) {
}
