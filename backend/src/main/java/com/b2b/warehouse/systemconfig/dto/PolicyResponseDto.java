package com.b2b.warehouse.systemconfig.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PolicyResponseDto(
        String policyHtml,
        Instant updatedAt
) {
}
