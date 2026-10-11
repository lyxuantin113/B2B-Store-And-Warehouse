package com.b2b.warehouse.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Represents a dynamic wholesale price tier configured by the Admin.
 * Both {@code minQty} and {@code maxQty} are freely set by Admin per product (not hardcoded).
 * When {@code maxQty} is {@code null}, the tier represents an open-ended bracket ({@code >= minQty}).
 */
@Builder
public record PriceTierDto(
        Long id,

        @Size(max = 60, message = "Tier code/label must not exceed 60 characters")
        String tierCode,

        String rangeLabel,

        @NotNull(message = "minQty is required")
        @Min(value = 1, message = "minQty must be at least 1")
        Integer minQty,

        Integer maxQty,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.00", message = "unitPrice must be >= 0")
        BigDecimal unitPrice,

        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00")
        BigDecimal discountPercent
) {
}
