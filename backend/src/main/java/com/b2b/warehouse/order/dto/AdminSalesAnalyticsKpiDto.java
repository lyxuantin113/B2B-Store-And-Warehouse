package com.b2b.warehouse.order.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * 6 KPI summary cards for Admin Sales &amp; Orders Analytics Dashboard (b2b-analytics.php).
 */
@Builder
public record AdminSalesAnalyticsKpiDto(
        BigDecimal totalRevenue,
        long totalOrders,
        BigDecimal averageOrderValue,
        long paidOrdersCount,
        BigDecimal deliveryCompletionRatePercent,
        BigDecimal cancellationRatePercent
) {
}
