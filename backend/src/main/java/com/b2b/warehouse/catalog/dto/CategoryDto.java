package com.b2b.warehouse.catalog.dto;

import lombok.Builder;

@Builder
public record CategoryDto(
        Long id,
        Long parentId,
        String parentName,
        String slug,
        String name,
        int sortOrder,
        boolean isActive
) {
}
