package com.b2b.warehouse.branch.controller;

import com.b2b.warehouse.branch.dto.BranchResponseDto;
import com.b2b.warehouse.branch.dto.UpsertBranchRequestDto;
import com.b2b.warehouse.branch.service.BranchService;
import com.b2b.warehouse.common.dto.ApiResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/branches")
@RequiredArgsConstructor
@Tag(name = "05. Admin Multi-Branch Warehouses", description = "Multi-Location Store & Warehouse Management (PostGIS Coordinates)")
public class AdminBranchController {

    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    @Operation(summary = "List all warehouse/store branches (active and inactive)")
    public ApiResponse<List<BranchResponseDto>> listAllBranches() {
        return ApiResponse.ok(branchService.listAllBranchesForAdmin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('branch:write')")
    @Operation(summary = "Create a new warehouse/store branch with PostGIS coordinates")
    public ApiResponse<BranchResponseDto> createBranch(@Valid @RequestBody UpsertBranchRequestDto request) {
        return ApiResponse.ok(branchService.createBranch(request), "Branch created successfully");
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("hasAuthority('branch:write')")
    @Operation(summary = "Update an existing warehouse/store branch")
    public ApiResponse<BranchResponseDto> updateBranch(
            @PathVariable UUID branchId,
            @Valid @RequestBody UpsertBranchRequestDto request
    ) {
        return ApiResponse.ok(branchService.updateBranch(branchId, request), "Branch updated successfully");
    }
}
