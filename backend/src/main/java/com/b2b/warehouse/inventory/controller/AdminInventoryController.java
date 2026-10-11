package com.b2b.warehouse.inventory.controller;

import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.inventory.dto.BranchInventoryDto;
import com.b2b.warehouse.inventory.dto.UpsertBranchInventoryRequestDto;
import com.b2b.warehouse.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@Tag(name = "06. Admin Multi-Branch Inventory", description = "Per-Branch Stock Levels, Safety Stock Thresholds & Low-Stock Alerts")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/branches/{branchId}")
    @PreAuthorize("hasAuthority('inventory:read')")
    @Operation(summary = "List stock levels of all products at a specific warehouse branch")
    public ApiResponse<List<BranchInventoryDto>> getBranchInventory(@PathVariable UUID branchId) {
        return ApiResponse.ok(inventoryService.getInventoryByBranch(branchId));
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('inventory:read')")
    @Operation(summary = "List stock levels of a product across all warehouse branches")
    public ApiResponse<List<BranchInventoryDto>> getProductInventoryAcrossBranches(@PathVariable UUID productId) {
        return ApiResponse.ok(inventoryService.getInventoryByProduct(productId));
    }

    @GetMapping("/low-stock-alerts")
    @PreAuthorize("hasAuthority('inventory:read')")
    @Operation(summary = "List all branch-product pairs where available_qty <= safety_stock")
    public ApiResponse<List<BranchInventoryDto>> getLowStockAlerts() {
        return ApiResponse.ok(inventoryService.getLowStockAlerts());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    @Operation(summary = "Set or adjust available stock and safety stock threshold for a (branch, product) pair")
    public ApiResponse<BranchInventoryDto> upsertBranchStock(@Valid @RequestBody UpsertBranchInventoryRequestDto request) {
        return ApiResponse.ok(
                inventoryService.upsertBranchStock(request),
                "Branch inventory updated successfully"
        );
    }
}
