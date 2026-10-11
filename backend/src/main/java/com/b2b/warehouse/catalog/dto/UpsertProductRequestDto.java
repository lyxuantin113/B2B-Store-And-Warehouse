package com.b2b.warehouse.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
public record UpsertProductRequestDto(
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "SKU is required")
        @Pattern(regexp = "^[A-Za-z0-9_\\-]{2,60}$", message = "SKU must be 2-60 alphanumeric/hyphen characters")
        String sku,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9\\-]{2,180}$", message = "Slug must be lowercase alphanumeric and hyphens")
        String slug,

        @NotBlank(message = "Product name is required")
        @Size(max = 200)
        String name,

        @Size(max = 255)
        String subtitle,

        String description,

        @NotBlank(message = "Unit label is required (e.g. Box (20ea))")
        @Size(max = 40)
        String unitLabel,

        @NotNull(message = "Units per box is required")
        @Min(value = 1)
        Integer unitsPerBox,

        @NotNull(message = "Retail price is required")
        @DecimalMin(value = "0.00")
        BigDecimal retailPrice,

        @NotNull(message = "Base wholesale price is required")
        @DecimalMin(value = "0.00")
        BigDecimal baseWholesalePrice,

        Boolean sampleEnabled,

        @Min(value = 1)
        Integer sampleMaxQty,

        @Min(value = 1)
        Integer weightGrams,

        @Size(max = 500)
        String thumbnailUrl,

        List<String> galleryUrls,

        Map<String, Object> specsJson,

        Boolean isActive,

        /**
         * Dynamic quantity price tiers configured by Admin (e.g. 1~5, 6~20, 21+ or 1~3, 4~6, 7+ or any custom intervals).
         */
        @Valid
        List<PriceTierDto> priceTiers
) {
}
