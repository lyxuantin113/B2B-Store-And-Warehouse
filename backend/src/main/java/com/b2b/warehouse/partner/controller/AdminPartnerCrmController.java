package com.b2b.warehouse.partner.controller;

import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.partner.dto.AdminBulkPartnerStatusRequestDto;
import com.b2b.warehouse.partner.dto.AdminPartnerKpiDto;
import com.b2b.warehouse.partner.dto.AdminPartnerStatusUpdateRequestDto;
import com.b2b.warehouse.partner.dto.AdminUpdatePartnerProfileRequestDto;
import com.b2b.warehouse.partner.dto.PartnerProfileResponseDto;
import com.b2b.warehouse.partner.service.B2bPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/partners")
@RequiredArgsConstructor
@Tag(name = "03. Admin B2B Partner CRM", description = "Admin/Manager Partner Approval Workflow, 4 KPI Cards, Bulk Status & Full Profile Edit")
public class AdminPartnerCrmController {

    private final B2bPartnerService partnerService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAuthority('partner:read')")
    @Operation(summary = "Get 4 KPI summary cards for Admin B2B Partner CRM dashboard")
    public ApiResponse<AdminPartnerKpiDto> getPartnerKpis() {
        return ApiResponse.ok(partnerService.getAdminPartnerKpis());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('partner:read')")
    @Operation(summary = "Search and filter B2B Partner applications by status and keyword")
    public ApiResponse<List<PartnerProfileResponseDto>> searchPartners(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PartnerProfileResponseDto> result = partnerService.searchPartnersForAdmin(
                status,
                keyword,
                PageRequest.of(page, Math.min(size, 100))
        );
        Map<String, Object> meta = Map.of(
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
        return ApiResponse.ok(result.getContent(), meta);
    }

    @PatchMapping("/{partnerId}/status")
    @PreAuthorize("hasAuthority('partner:approve')")
    @Operation(summary = "Approve (ADMIN_APPROVED / ACTIVE), Reject (REJECTED), or Suspend (SUSPENDED) a B2B Partner")
    public ApiResponse<PartnerProfileResponseDto> updatePartnerStatus(
            @PathVariable UUID partnerId,
            @AuthenticationPrincipal AuthenticatedUser adminUser,
            @Valid @RequestBody AdminPartnerStatusUpdateRequestDto request
    ) {
        PartnerProfileResponseDto updated = partnerService.updatePartnerStatusByAdmin(
                partnerId,
                adminUser.getUserId(),
                request
        );
        return ApiResponse.ok(updated, "B2B Partner status updated to " + request.targetStatus());
    }

    @PostMapping("/bulk-status")
    @PreAuthorize("hasAuthority('partner:approve')")
    @Operation(summary = "Bulk approve, activate, reject, or suspend multiple B2B Partners")
    public ApiResponse<List<PartnerProfileResponseDto>> bulkUpdateStatus(
            @AuthenticationPrincipal AuthenticatedUser adminUser,
            @Valid @RequestBody AdminBulkPartnerStatusRequestDto request
    ) {
        List<PartnerProfileResponseDto> updated = partnerService.bulkUpdatePartnerStatusByAdmin(
                adminUser.getUserId(),
                request
        );
        return ApiResponse.ok(updated, "Bulk updated " + updated.size() + " B2B partners to " + request.targetStatus());
    }

    @PutMapping("/{partnerId}")
    @PreAuthorize("hasAuthority('partner:approve')")
    @Operation(summary = "Admin full update of B2B Partner profile including verified legal fields")
    public ApiResponse<PartnerProfileResponseDto> updatePartnerProfile(
            @PathVariable UUID partnerId,
            @Valid @RequestBody AdminUpdatePartnerProfileRequestDto request
    ) {
        PartnerProfileResponseDto updated = partnerService.updatePartnerProfileByAdmin(partnerId, request);
        return ApiResponse.ok(updated, "B2B Partner profile updated by Admin");
    }
}
