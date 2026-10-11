package com.b2b.warehouse.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpsertBranchInventoryRequestDto(
        @NotNull(message = "Branch ID is required")
        UUID branchId,

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Available quantity is required")
        @Min(value = 0, message = "Available quantity cannot be negative")
        Integer availableQty,

        @Min(value = 0, message = "Safety stock cannot be negative")
        Integer safetyStock
) {
}
