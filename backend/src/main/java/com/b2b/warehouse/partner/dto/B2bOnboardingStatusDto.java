package com.b2b.warehouse.partner.dto;

import com.b2b.warehouse.auth.entity.AccountStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for the 3-Stage B2B Onboarding Status Tracker ({@code GET /api/v1/b2b/onboarding/status}).
 */
@Builder
public record B2bOnboardingStatusDto(
        UUID partnerProfileId,
        UUID userId,
        String email,
        AccountStatus accountStatus,
        int currentStep,
        String companyName,
        String taxCode,
        String businessType,
        String representativeName,
        String contactPersonName,
        String contactPhone,
        String businessLicenseUrl,
        String postalCode,
        String addressLine,
        String addressDetail,
        String approvalNotice,
        String rejectionReason,
        int reapplyCount,
        Instant approvedAt,
        Instant partnerConfirmedAt,
        Instant submittedAt
) {
}
