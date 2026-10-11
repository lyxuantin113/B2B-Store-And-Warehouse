package com.b2b.warehouse.rbac.controller;

import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.rbac.dto.CreateStaffAccountRequestDto;
import com.b2b.warehouse.rbac.dto.RolePermissionsResponseDto;
import com.b2b.warehouse.rbac.dto.UpdateRolePermissionsRequestDto;
import com.b2b.warehouse.rbac.service.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/rbac")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('rbac:manage')")
@Tag(name = "04. Admin RBAC & Staff Accounts", description = "Role-Permission Matrix Configuration & Staff Account Provisioning (ADMIN, MANAGER, SELLER)")
public class AdminRbacController {

    private final RbacService rbacService;

    @GetMapping("/roles")
    @Operation(summary = "List all RBAC roles and their assigned granular permissions")
    public ApiResponse<List<RolePermissionsResponseDto>> getRoles() {
        return ApiResponse.ok(rbacService.getAllRolesWithPermissions());
    }

    @PutMapping("/roles/{roleCode}/permissions")
    @Operation(summary = "Update granular permissions assigned to a role (MANAGER, SELLER, B2B_PARTNER)")
    public ApiResponse<RolePermissionsResponseDto> updateRolePermissions(
            @PathVariable String roleCode,
            @Valid @RequestBody UpdateRolePermissionsRequestDto request
    ) {
        return ApiResponse.ok(
                rbacService.updateRolePermissions(roleCode, request),
                "Updated permissions for role " + roleCode.toUpperCase()
        );
    }

    @PostMapping("/staff-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an internal ADMIN, MANAGER, or SELLER staff account")
    public ApiResponse<Map<String, Object>> createStaffAccount(@Valid @RequestBody CreateStaffAccountRequestDto request) {
        return ApiResponse.ok(rbacService.createStaffAccount(request), "Staff account created in ACTIVE status");
    }
}
