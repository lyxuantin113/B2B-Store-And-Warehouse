package com.b2b.warehouse.catalog.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Live wholesale price quote calculated dynamically from the product's Admin-configured quantity tiers.
 */
@Builder
public record WholesalePriceQuoteDto(
        UUID productId,
        String sku,
        String productName,
        String unitLabel,
        int quantity,
        String appliedTierCode,
        String appliedRangeLabel,
        BigDecimal retailPrice,
        BigDecimal baseWholesalePrice,
        BigDecimal appliedUnitPrice,
        BigDecimal lineTotal,
        BigDecimal totalSavingsVsRetail
) {
}
