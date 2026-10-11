package com.b2b.warehouse.branch.controller;

import com.b2b.warehouse.branch.dto.BranchResponseDto;
import com.b2b.warehouse.branch.dto.NearbyBranchResponseDto;
import com.b2b.warehouse.branch.dto.StandardizedAddressDto;
import com.b2b.warehouse.branch.service.BranchService;
import com.b2b.warehouse.branch.service.GoogleMapsGeocodingService;
import com.b2b.warehouse.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public &amp; B2B Store Locator Controller (replaces {@code stores.php} + Korean Daum/Naver Map).
 * Supports Google Maps address standardization/geocoding and PostGIS nearest branch search within a radius.
 */
@RestController
@RequestMapping({"/api/v1/public/store-locator", "/api/v1/b2b/store-locator"})
@RequiredArgsConstructor
@Tag(name = "Store Locator & Google Maps Geocoding", description = "Standardize Vietnamese addresses and search nearest warehouse/store branches by radius")
public class PublicStoreLocatorController {

    private final BranchService branchService;
    private final GoogleMapsGeocodingService googleMapsGeocodingService;

    @GetMapping("/geocode")
    @Operation(summary = "Standardize a Vietnamese address and resolve WGS84 (latitude, longitude) coordinates via Google Maps")
    public ResponseEntity<ApiResponse<StandardizedAddressDto>> geocodeAddress(
            @RequestParam("address") String address
    ) {
        return ResponseEntity.ok(ApiResponse.ok(googleMapsGeocodingService.standardizeAddress(address)));
    }

    @GetMapping("/nearest")
    @Operation(summary = "Search nearest active branches within radiusKm by (lat, lng) or Vietnamese address string")
    public ResponseEntity<ApiResponse<List<NearbyBranchResponseDto>>> searchNearestBranches(
            @RequestParam(name = "lat", required = false) Double latitude,
            @RequestParam(name = "lng", required = false) Double longitude,
            @RequestParam(name = "address", required = false) String addressQuery,
            @RequestParam(name = "radiusKm", defaultValue = "10.0") Double radiusKm,
            @RequestParam(name = "categoryTag", required = false) String categoryTag,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        List<NearbyBranchResponseDto> results = branchService.searchNearestBranches(
                latitude, longitude, addressQuery, radiusKm, categoryTag, keyword
        );
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/branches")
    @Operation(summary = "List all active branches for initial map marker rendering")
    public ResponseEntity<ApiResponse<List<BranchResponseDto>>> listActiveBranches() {
        return ResponseEntity.ok(ApiResponse.ok(branchService.listActiveBranches()));
    }
}
