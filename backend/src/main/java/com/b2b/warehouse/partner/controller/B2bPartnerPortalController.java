package com.b2b.warehouse.partner.controller;

import com.b2b.warehouse.auth.dto.AuthTokenResponseDto;
import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.partner.dto.B2bOnboardingStatusDto;
import com.b2b.warehouse.partner.dto.B2bReapplyRequestDto;
import com.b2b.warehouse.partner.dto.PartnerProfileResponseDto;
import com.b2b.warehouse.partner.dto.PartnerSelfUpdateProfileRequestDto;
import com.b2b.warehouse.partner.service.B2bPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/b2b")
@RequiredArgsConstructor
@PreAuthorize("hasRole('B2B_PARTNER')")
@Tag(name = "02. B2B Partner Onboarding & Profile", description = "3-Stage Onboarding Tracker, Re-apply, Final Confirmation & Tab 8 Company Profile")
public class B2bPartnerPortalController {

    private final B2bPartnerService partnerService;

    @GetMapping("/onboarding/status")
    @Operation(summary = "Get 3-Step B2B Onboarding Status Tracker (Accessible in PENDING_ADMIN, REJECTED, ADMIN_APPROVED, ACTIVE)")
    public ApiResponse<B2bOnboardingStatusDto> getOnboardingStatus(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(partnerService.getOnboardingStatus(currentUser.getUserId()));
    }

    @PutMapping("/onboarding/reapply")
    @Operation(summary = "Stage 1 Re-apply: Edit and resubmit company profile after REJECTED status")
    public ApiResponse<B2bOnboardingStatusDto> reapply(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody B2bReapplyRequestDto request
    ) {
        B2bOnboardingStatusDto updated = partnerService.reapply(currentUser.getUserId(), request);
        return ApiResponse.ok(updated, "Application resubmitted successfully. Status is now PENDING_ADMIN.");
    }

    @PostMapping("/onboarding/confirm")
    @Operation(summary = "Stage 2 -> Stage 3 Final Confirmation: Confirm ADMIN_APPROVED profile to unlock ACTIVE B2B Portal")
    public ApiResponse<AuthTokenResponseDto> confirmOnboarding(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        AuthTokenResponseDto activeTokens = partnerService.confirmOnboardingAndActivate(currentUser.getUserId());
        return ApiResponse.ok(activeTokens, "Your B2B Partner account is now ACTIVE. Welcome to the B2B Wholesale Portal!");
    }

    @GetMapping("/profile")
    @Operation(summary = "B2B Portal Tab 8: Get active B2B Partner company profile")
    public ApiResponse<PartnerProfileResponseDto> getMyProfile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(partnerService.getMyProfile(currentUser.getUserId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "B2B Portal Tab 8: Update operational contact & shipping address (Legal fields remain read-only)")
    public ApiResponse<PartnerProfileResponseDto> updateMyProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody PartnerSelfUpdateProfileRequestDto request
    ) {
        PartnerProfileResponseDto updated = partnerService.updateMyOperationalProfile(currentUser.getUserId(), request);
        return ApiResponse.ok(updated, "Operational contact and default shipping address updated.");
    }
}
