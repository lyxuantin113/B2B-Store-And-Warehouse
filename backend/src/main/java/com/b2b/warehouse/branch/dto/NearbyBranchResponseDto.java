package com.b2b.warehouse.branch.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record NearbyBranchResponseDto(
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
        double distanceKm,
        long distanceMeters
) {
}
