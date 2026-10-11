package com.b2b.warehouse.catalog.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verified B2B Partner &amp; Admin Catalog product view including Admin-configured dynamic price tiers.
 */
@Builder
public record B2bProductResponseDto(
        UUID id,
        Long categoryId,
        String categoryName,
        String sku,
        String slug,
        String name,
        String subtitle,
        String description,
        String unitLabel,
        int unitsPerBox,
        BigDecimal retailPrice,
        BigDecimal baseWholesalePrice,
        boolean sampleEnabled,
        int sampleMaxQty,
        int weightGrams,
        String thumbnailUrl,
        List<String> galleryUrls,
        Map<String, Object> specsJson,
        boolean isActive,
        int totalAvailableStock,
        List<PriceTierDto> priceTiers,
        Instant createdAt,
        Instant updatedAt
) {
}
