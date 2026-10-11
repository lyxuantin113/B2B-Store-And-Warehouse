package com.b2b.warehouse.rbac.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateRolePermissionsRequestDto(
        @NotNull(message = "Permission codes set is required")
        Set<String> permissionCodes
) {
}
