package com.b2b.warehouse.partner.dto;

import lombok.Builder;

/**
 * 4 KPI summary cards displayed at the top of the Admin B2B Partner CRM dashboard ({@code b2b-admin.php}).
 */
@Builder
public record AdminPartnerKpiDto(
        long totalPartners,
        long pendingReviewCount,
        long awaitingConfirmationCount,
        long activeCount,
        long rejectedCount,
        long suspendedCount
) {
}
