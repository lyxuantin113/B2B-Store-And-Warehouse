package com.b2b.warehouse.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpsertCategoryRequestDto(
        Long parentId,

        @NotBlank(message = "Category slug is required")
        @Pattern(regexp = "^[a-z0-9\\-]{2,120}$", message = "Slug must be lowercase alphanumeric and hyphens")
        String slug,

        @NotBlank(message = "Category name is required")
        @Size(max = 120)
        String name,

        Integer sortOrder,
        Boolean isActive
) {
}
