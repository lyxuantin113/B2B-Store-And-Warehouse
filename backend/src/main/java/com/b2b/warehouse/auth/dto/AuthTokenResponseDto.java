package com.b2b.warehouse.auth.dto;

import com.b2b.warehouse.auth.entity.AccountStatus;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Dual-token authentication response containing short-lived Access Token,
 * rotated Refresh Token, RBAC permissions, and current 3-stage onboarding status.
 */
@Builder
public record AuthTokenResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        String email,
        String fullName,
        String role,
        AccountStatus accountStatus,
        int onboardingStep,
        List<String> permissions
) {
}
