package com.b2b.warehouse.order.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tab 1 Overview KPIs and recent orders for the B2B Partner Portal (b2b-portal.php).
 */
@Builder
public record PartnerDashboardOverviewDto(
        BigDecimal totalCumulativeSpend,
        long totalOrdersCount,
        long activeInTransitOrdersCount,
        long sampleOrdersCount,
        List<OrderResponseDto> recentOrders
) {
}
