package com.b2b.warehouse.inventory.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record BranchInventoryDto(
        Long id,
        UUID branchId,
        String branchCode,
        String branchName,
        UUID productId,
        String productSku,
        String productName,
        String unitLabel,
        int availableQty,
        int reservedQty,
        int safetyStock,
        boolean lowStockWarning,
        Instant updatedAt
) {
}
