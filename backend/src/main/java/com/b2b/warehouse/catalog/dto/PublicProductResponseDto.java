package com.b2b.warehouse.catalog.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public Storefront product view.
 * Intentionally omits B2B wholesale prices and tier brackets for unauthenticated visitors.
 */
@Builder
public record PublicProductResponseDto(
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
        boolean sampleEnabled,
        String thumbnailUrl,
        List<String> galleryUrls,
        Map<String, Object> specsJson,
        Instant createdAt
) {
}
