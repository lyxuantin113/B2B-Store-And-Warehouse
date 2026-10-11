package com.b2b.warehouse.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record OrderLineItemRequestDto(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {
}
