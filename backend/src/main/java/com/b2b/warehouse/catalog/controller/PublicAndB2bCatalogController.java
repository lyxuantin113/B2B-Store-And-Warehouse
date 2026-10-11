package com.b2b.warehouse.catalog.controller;

import com.b2b.warehouse.catalog.dto.B2bProductResponseDto;
import com.b2b.warehouse.catalog.dto.CategoryDto;
import com.b2b.warehouse.catalog.dto.PublicProductResponseDto;
import com.b2b.warehouse.catalog.dto.WholesalePriceQuoteDto;
import com.b2b.warehouse.catalog.service.CatalogService;
import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.common.dto.CursorPageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "08. Public Storefront & B2B Wholesale Catalog", description = "Keyset-Paginated Catalog & Live Dynamic Quantity Price Quote")
public class PublicAndB2bCatalogController {

    private final CatalogService catalogService;

    @GetMapping("/api/v1/public/catalog/categories")
    @Operation(summary = "List active product categories for Public Storefront and B2B Portal")
    public ApiResponse<List<CategoryDto>> listActiveCategories() {
        return ApiResponse.ok(catalogService.listActiveCategories());
    }

    @GetMapping("/api/v1/public/catalog/products")
    @Operation(summary = "Keyset-paginated Public Storefront catalog (hides B2B wholesale prices)")
    public ApiResponse<CursorPageDto<PublicProductResponseDto>> listPublicProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Instant cursorCreatedAt,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(catalogService.listPublicProductsKeyset(
                categoryId,
                keyword,
                cursorCreatedAt,
                cursorId,
                limit
        ));
    }

    @GetMapping("/api/v1/b2b/catalog/products")
    @Operation(summary = "Keyset-paginated B2B Portal catalog & Quick Order Matrix with Admin-configured dynamic price tiers and stock")
    public ApiResponse<CursorPageDto<B2bProductResponseDto>> listB2bProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Instant cursorCreatedAt,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(catalogService.listB2bProductsKeyset(
                categoryId,
                keyword,
                cursorCreatedAt,
                cursorId,
                limit
        ));
    }

    @GetMapping("/api/v1/b2b/catalog/products/{productId}/quote")
    @Operation(summary = "Calculate live wholesale unit price and line total for a given quantity based on dynamic Admin price tiers")
    public ApiResponse<WholesalePriceQuoteDto> calculateWholesaleQuote(
            @PathVariable UUID productId,
            @RequestParam int quantity
    ) {
        return ApiResponse.ok(catalogService.calculateWholesaleQuote(productId, quantity));
    }
}
