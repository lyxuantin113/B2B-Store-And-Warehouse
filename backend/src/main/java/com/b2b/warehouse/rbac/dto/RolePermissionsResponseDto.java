package com.b2b.warehouse.rbac.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RolePermissionsResponseDto(
        Short roleId,
        String roleCode,
        String roleName,
        String description,
        List<String> permissions
) {
}
