package com.b2b.warehouse.catalog.controller;

import com.b2b.warehouse.catalog.dto.B2bProductResponseDto;
import com.b2b.warehouse.catalog.dto.CategoryDto;
import com.b2b.warehouse.catalog.dto.PriceTierDto;
import com.b2b.warehouse.catalog.dto.UpsertCategoryRequestDto;
import com.b2b.warehouse.catalog.dto.UpsertProductRequestDto;
import com.b2b.warehouse.catalog.service.CatalogService;
import com.b2b.warehouse.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
@Tag(name = "07. Admin Catalog & Dynamic Price Tiers", description = "Categories, Products & Admin-Configurable Dynamic Quantity Price Brackets")
public class AdminCatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('inventory:read')")
    @Operation(summary = "List all categories (active and inactive)")
    public ApiResponse<List<CategoryDto>> listAllCategories() {
        return ApiResponse.ok(catalogService.listAllCategoriesForAdmin());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('catalog:write')")
    @Operation(summary = "Create a new product category")
    public ApiResponse<CategoryDto> createCategory(@Valid @RequestBody UpsertCategoryRequestDto request) {
        return ApiResponse.ok(catalogService.createCategory(request), "Category created successfully");
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('catalog:write')")
    @Operation(summary = "Create a product with Admin-configured dynamic quantity price tiers")
    public ApiResponse<B2bProductResponseDto> createProduct(@Valid @RequestBody UpsertProductRequestDto request) {
        return ApiResponse.ok(catalogService.createProduct(request), "Product and dynamic price tiers created");
    }

    @PutMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('catalog:write')")
    @Operation(summary = "Update a product and its Admin-configured dynamic quantity price tiers")
    public ApiResponse<B2bProductResponseDto> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody UpsertProductRequestDto request
    ) {
        return ApiResponse.ok(catalogService.updateProduct(productId, request), "Product updated successfully");
    }

    @PutMapping("/products/{productId}/price-tiers")
    @PreAuthorize("hasAuthority('catalog:write')")
    @Operation(summary = "Dynamically reconfigure the quantity price brackets (minQty ~ maxQty & unitPrice) for a product")
    public ApiResponse<B2bProductResponseDto> replacePriceTiers(
            @PathVariable UUID productId,
            @Valid @RequestBody List<PriceTierDto> priceTiers
    ) {
        return ApiResponse.ok(
                catalogService.replaceProductPriceTiers(productId, priceTiers),
                "Dynamic quantity price tiers updated"
        );
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('catalog:write')")
    @Operation(summary = "Soft-delete and deactivate a product")
    public ApiResponse<Void> deleteProduct(@PathVariable UUID productId) {
        catalogService.softDeleteProduct(productId);
        return ApiResponse.ok(null, "Product deactivated and soft-deleted");
    }
}
