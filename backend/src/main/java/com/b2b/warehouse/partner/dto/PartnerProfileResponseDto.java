package com.b2b.warehouse.partner.dto;

import com.b2b.warehouse.auth.entity.AccountStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Full B2B Partner profile representation used in B2B Portal Tab 8 and Admin Partner CRM.
 */
@Builder
public record PartnerProfileResponseDto(
        UUID partnerId,
        UUID userId,
        String email,
        AccountStatus accountStatus,
        // Immutable legal fields (editable only by Admin)
        String companyName,
        String taxCode,
        String businessType,
        String representativeName,
        String businessLicenseUrl,
        String postalCode,
        String addressLine,
        String addressDetail,
        // Editable operational & shipping fields
        String contactPersonName,
        String contactPhone,
        String shippingPostalCode,
        String shippingAddressLine,
        String shippingAddressDetail,
        // Audit & onboarding metadata
        String approvalNotice,
        String rejectionReason,
        int reapplyCount,
        Instant approvedAt,
        Instant partnerConfirmedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
