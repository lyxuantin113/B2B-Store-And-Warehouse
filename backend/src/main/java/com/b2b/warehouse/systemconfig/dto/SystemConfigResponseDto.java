package com.b2b.warehouse.systemconfig.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record SystemConfigResponseDto(
        String configKey,
        String configGroup,
        String configValue,
        boolean isEncrypted,
        UUID updatedBy,
        Instant updatedAt
) {
}
