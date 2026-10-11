package com.b2b.warehouse.order.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderItemResponseDto(
        Long id,
        UUID productId,
        String sku,
        String productName,
        String unitLabel,
        String appliedTierCode,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
