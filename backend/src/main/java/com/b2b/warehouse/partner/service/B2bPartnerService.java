package com.b2b.warehouse.partner.service;

import com.b2b.warehouse.auth.dto.AuthTokenResponseDto;
import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.auth.repository.UserRepository;
import com.b2b.warehouse.auth.service.AuthService;
import com.b2b.warehouse.auth.service.RefreshTokenStoreService;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.common.util.HtmlSanitizerUtil;
import com.b2b.warehouse.partner.dto.AdminBulkPartnerStatusRequestDto;
import com.b2b.warehouse.partner.dto.AdminPartnerKpiDto;
import com.b2b.warehouse.partner.dto.AdminPartnerStatusUpdateRequestDto;
import com.b2b.warehouse.partner.dto.AdminUpdatePartnerProfileRequestDto;
import com.b2b.warehouse.partner.dto.B2bOnboardingStatusDto;
import com.b2b.warehouse.partner.dto.B2bReapplyRequestDto;
import com.b2b.warehouse.partner.dto.PartnerProfileResponseDto;
import com.b2b.warehouse.partner.dto.PartnerSelfUpdateProfileRequestDto;
import com.b2b.warehouse.partner.entity.B2bPartnerProfileEntity;
import com.b2b.warehouse.partner.repository.B2bPartnerProfileRepository;
import com.b2b.warehouse.rbac.entity.RoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the 3-Stage B2B Partner Onboarding state machine,
 * Partner Profile management (with immutable legal fields), and Admin Partner CRM.
 */
@Service
@RequiredArgsConstructor
public class B2bPartnerService {

    private final B2bPartnerProfileRepository partnerProfileRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final RefreshTokenStoreService refreshTokenStoreService;
    private final HtmlSanitizerUtil htmlSanitizerUtil;

    /**
     * Retrieves the 3-Step Onboarding Status Tracker state for the logged-in B2B Partner.
     */
    @Transactional(readOnly = true)
    public B2bOnboardingStatusDto getOnboardingStatus(UUID userId) {
        B2bPartnerProfileEntity profile = findProfileByUserIdOrThrow(userId);
        return toOnboardingStatusDto(profile);
    }

    /**
     * Stage 1 Re-apply: Allows a B2B Partner whose application was {@code REJECTED}
     * to update their company details and business license in-place and re-enter {@code PENDING_ADMIN}.
     */
    @Transactional
    public B2bOnboardingStatusDto reapply(UUID userId, B2bReapplyRequestDto request) {
        B2bPartnerProfileEntity profile = findProfileByUserIdOrThrow(userId);
        UserEntity user = profile.getUser();

        if (user.getAccountStatus() != AccountStatus.REJECTED) {
            throw new BusinessException(
                    ErrorCode.INVALID_ONBOARDING_STATE,
                    "Re-application is only permitted when account status is REJECTED"
            );
        }

        String normalizedTaxCode = request.taxCode().trim();
        if (partnerProfileRepository.existsByTaxCodeAndUserIdNot(normalizedTaxCode, userId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TAX_CODE);
        }

        profile.setCompanyName(request.companyName().trim());
        profile.setTaxCode(normalizedTaxCode);
        profile.setBusinessType(request.businessType());
        profile.setRepresentativeName(request.representativeName().trim());
        profile.setContactPersonName(request.contactPersonName().trim());
        profile.setContactPhone(request.contactPhone().trim());
        profile.setBusinessLicenseUrl(request.businessLicenseUrl().trim());
        profile.setPostalCode(request.postalCode());
        profile.setAddressLine(request.addressLine().trim());
        profile.setAddressDetail(request.addressDetail());
        profile.setRejectionReason(null);
        profile.setReapplyCount((profile.getReapplyCount() == null ? 0 : profile.getReapplyCount()) + 1);

        user.setFullName(request.contactPersonName().trim());
        user.setPhone(request.contactPhone().trim());
        user.setAccountStatus(AccountStatus.PENDING_ADMIN);

        partnerProfileRepository.save(profile);
        return toOnboardingStatusDto(profile);
    }

    /**
     * Stage 2 -> Stage 3 Final Confirmation:
     * When Admin has approved the application ({@code ADMIN_APPROVED}), the B2B Partner reviews
     * the verified company details + Admin's {@code approvalNotice} and clicks Confirm to transition to {@code ACTIVE}.
     * Immediately issues a fresh JWT token pair containing {@code status = ACTIVE}.
     */
    @Transactional
    public AuthTokenResponseDto confirmOnboardingAndActivate(UUID userId) {
        B2bPartnerProfileEntity profile = findProfileByUserIdOrThrow(userId);
        UserEntity user = userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (user.getAccountStatus() != AccountStatus.ADMIN_APPROVED) {
            throw new BusinessException(
                    ErrorCode.INVALID_ONBOARDING_STATE,
                    "Account must be in ADMIN_APPROVED (Stage 2) status to complete final confirmation"
            );
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        profile.setPartnerConfirmedAt(Instant.now());

        userRepository.save(user);
        partnerProfileRepository.save(profile);

        return authService.createNewSessionTokens(user);
    }

    /**
     * B2B Portal Tab 8: Retrieves full company profile for an active B2B Partner.
     */
    @Transactional(readOnly = true)
    public PartnerProfileResponseDto getMyProfile(UUID userId) {
        B2bPartnerProfileEntity profile = findProfileByUserIdOrThrow(userId);
        return toPartnerProfileDto(profile);
    }

    /**
     * B2B Portal Tab 8: Updates only operational contact &amp; default shipping address fields.
     * Legal registration fields ({@code companyName}, {@code taxCode}, {@code representativeName}) remain locked.
     */
    @Transactional
    public PartnerProfileResponseDto updateMyOperationalProfile(UUID userId, PartnerSelfUpdateProfileRequestDto request) {
        B2bPartnerProfileEntity profile = findProfileByUserIdOrThrow(userId);
        UserEntity user = profile.getUser();

        profile.setContactPersonName(request.contactPersonName().trim());
        profile.setContactPhone(request.contactPhone().trim());
        profile.setShippingPostalCode(request.shippingPostalCode());
        profile.setShippingAddressLine(request.shippingAddressLine().trim());
        profile.setShippingAddressDetail(request.shippingAddressDetail());

        user.setFullName(request.contactPersonName().trim());
        user.setPhone(request.contactPhone().trim());

        partnerProfileRepository.save(profile);
        return toPartnerProfileDto(profile);
    }

    /**
     * Admin Partner CRM: Returns the 4 KPI summary counts.
     */
    @Transactional(readOnly = true)
    public AdminPartnerKpiDto getAdminPartnerKpis() {
        String roleCode = RoleCode.B2B_PARTNER.name();
        long total = userRepository.countByRole_CodeAndDeletedAtIsNull(roleCode);
        long pending = userRepository.countByRole_CodeAndAccountStatusAndDeletedAtIsNull(roleCode, AccountStatus.PENDING_ADMIN);
        long approved = userRepository.countByRole_CodeAndAccountStatusAndDeletedAtIsNull(roleCode, AccountStatus.ADMIN_APPROVED);
        long active = userRepository.countByRole_CodeAndAccountStatusAndDeletedAtIsNull(roleCode, AccountStatus.ACTIVE);
        long rejected = userRepository.countByRole_CodeAndAccountStatusAndDeletedAtIsNull(roleCode, AccountStatus.REJECTED);
        long suspended = userRepository.countByRole_CodeAndAccountStatusAndDeletedAtIsNull(roleCode, AccountStatus.SUSPENDED);

        return AdminPartnerKpiDto.builder()
                .totalPartners(total)
                .pendingReviewCount(pending)
                .awaitingConfirmationCount(approved)
                .activeCount(active)
                .rejectedCount(rejected)
                .suspendedCount(suspended)
                .build();
    }

    /**
     * Admin Partner CRM: Searches and filters B2B partners without N+1 queries.
     */
    @Transactional(readOnly = true)
    public Page<PartnerProfileResponseDto> searchPartnersForAdmin(AccountStatus status, String keyword, Pageable pageable) {
        return partnerProfileRepository.searchPartnersForAdmin(status, keyword, pageable)
                .map(this::toPartnerProfileDto);
    }

    /**
     * Admin Partner CRM: Updates a partner's onboarding/account status.
     */
    @Transactional
    public PartnerProfileResponseDto updatePartnerStatusByAdmin(
            UUID partnerId,
            UUID adminUserId,
            AdminPartnerStatusUpdateRequestDto request
    ) {
        B2bPartnerProfileEntity profile = partnerProfileRepository.findWithUserById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "B2B Partner profile not found"));

        applyStatusTransition(profile, adminUserId, request.targetStatus(), request.approvalNotice(), request.rejectionReason());
        partnerProfileRepository.save(profile);
        return toPartnerProfileDto(profile);
    }

    /**
     * Admin Partner CRM: Bulk updates multiple B2B partners' statuses in a single transaction.
     */
    @Transactional
    public List<PartnerProfileResponseDto> bulkUpdatePartnerStatusByAdmin(
            UUID adminUserId,
            AdminBulkPartnerStatusRequestDto request
    ) {
        List<B2bPartnerProfileEntity> profiles = partnerProfileRepository.findByIdIn(request.partnerIds());
        for (B2bPartnerProfileEntity profile : profiles) {
            applyStatusTransition(
                    profile,
                    adminUserId,
                    request.targetStatus(),
                    request.noticeOrReason(),
                    request.noticeOrReason()
            );
        }
        partnerProfileRepository.saveAll(profiles);
        return profiles.stream().map(this::toPartnerProfileDto).toList();
    }

    /**
     * Admin Partner CRM: Allows Admin/Manager to edit both legal and operational partner fields.
     */
    @Transactional
    public PartnerProfileResponseDto updatePartnerProfileByAdmin(
            UUID partnerId,
            AdminUpdatePartnerProfileRequestDto request
    ) {
        B2bPartnerProfileEntity profile = partnerProfileRepository.findWithUserById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "B2B Partner profile not found"));
        UserEntity user = profile.getUser();

        String normalizedTaxCode = request.taxCode().trim();
        if (partnerProfileRepository.existsByTaxCodeAndUserIdNot(normalizedTaxCode, user.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_TAX_CODE);
        }

        profile.setCompanyName(request.companyName().trim());
        profile.setTaxCode(normalizedTaxCode);
        profile.setBusinessType(request.businessType());
        profile.setRepresentativeName(request.representativeName().trim());
        profile.setContactPersonName(request.contactPersonName().trim());
        profile.setContactPhone(request.contactPhone().trim());
        profile.setBusinessLicenseUrl(request.businessLicenseUrl().trim());
        profile.setPostalCode(request.postalCode());
        profile.setAddressLine(request.addressLine().trim());
        profile.setAddressDetail(request.addressDetail());
        profile.setShippingPostalCode(request.shippingPostalCode());
        profile.setShippingAddressLine(request.shippingAddressLine());
        profile.setShippingAddressDetail(request.shippingAddressDetail());

        user.setFullName(request.contactPersonName().trim());
        user.setPhone(request.contactPhone().trim());

        partnerProfileRepository.save(profile);
        return toPartnerProfileDto(profile);
    }

    private void applyStatusTransition(
            B2bPartnerProfileEntity profile,
            UUID adminUserId,
            AccountStatus targetStatus,
            String approvalNotice,
            String rejectionReason
    ) {
        UserEntity user = profile.getUser();
        user.setAccountStatus(targetStatus);

        switch (targetStatus) {
            case ADMIN_APPROVED -> {
                profile.setApprovalNotice(htmlSanitizerUtil.sanitizeRichText(approvalNotice));
                profile.setRejectionReason(null);
                profile.setApprovedBy(adminUserId);
                profile.setApprovedAt(Instant.now());
            }
            case ACTIVE -> {
                if (approvalNotice != null && !approvalNotice.isBlank()) {
                    profile.setApprovalNotice(htmlSanitizerUtil.sanitizeRichText(approvalNotice));
                }
                profile.setRejectionReason(null);
                if (profile.getApprovedAt() == null) {
                    profile.setApprovedBy(adminUserId);
                    profile.setApprovedAt(Instant.now());
                }
                if (profile.getPartnerConfirmedAt() == null) {
                    profile.setPartnerConfirmedAt(Instant.now());
                }
            }
            case REJECTED -> {
                if (rejectionReason == null || rejectionReason.isBlank()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Rejection reason is required when rejecting a B2B application");
                }
                profile.setRejectionReason(htmlSanitizerUtil.sanitizeRichText(rejectionReason));
            }
            case SUSPENDED -> {
                user.incrementTokenVersion();
                refreshTokenStoreService.revokeAllUserSessions(user.getId());
            }
            case PENDING_ADMIN -> {
                // Reset to Stage 1
            }
        }
    }

    private B2bPartnerProfileEntity findProfileByUserIdOrThrow(UUID userId) {
        return partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "B2B Partner profile not found"));
    }

    private B2bOnboardingStatusDto toOnboardingStatusDto(B2bPartnerProfileEntity profile) {
        UserEntity user = profile.getUser();
        return B2bOnboardingStatusDto.builder()
                .partnerProfileId(profile.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .accountStatus(user.getAccountStatus())
                .currentStep(AuthService.resolveOnboardingStep(user.getAccountStatus()))
                .companyName(profile.getCompanyName())
                .taxCode(profile.getTaxCode())
                .businessType(profile.getBusinessType())
                .representativeName(profile.getRepresentativeName())
                .contactPersonName(profile.getContactPersonName())
                .contactPhone(profile.getContactPhone())
                .businessLicenseUrl(profile.getBusinessLicenseUrl())
                .postalCode(profile.getPostalCode())
                .addressLine(profile.getAddressLine())
                .addressDetail(profile.getAddressDetail())
                .approvalNotice(profile.getApprovalNotice())
                .rejectionReason(profile.getRejectionReason())
                .reapplyCount(profile.getReapplyCount() == null ? 0 : profile.getReapplyCount())
                .approvedAt(profile.getApprovedAt())
                .partnerConfirmedAt(profile.getPartnerConfirmedAt())
                .submittedAt(profile.getCreatedAt())
                .build();
    }

    private PartnerProfileResponseDto toPartnerProfileDto(B2bPartnerProfileEntity profile) {
        UserEntity user = profile.getUser();
        return PartnerProfileResponseDto.builder()
                .partnerId(profile.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .accountStatus(user.getAccountStatus())
                .companyName(profile.getCompanyName())
                .taxCode(profile.getTaxCode())
                .businessType(profile.getBusinessType())
                .representativeName(profile.getRepresentativeName())
                .businessLicenseUrl(profile.getBusinessLicenseUrl())
                .postalCode(profile.getPostalCode())
                .addressLine(profile.getAddressLine())
                .addressDetail(profile.getAddressDetail())
                .contactPersonName(profile.getContactPersonName())
                .contactPhone(profile.getContactPhone())
                .shippingPostalCode(profile.getShippingPostalCode())
                .shippingAddressLine(profile.getShippingAddressLine())
                .shippingAddressDetail(profile.getShippingAddressDetail())
                .approvalNotice(profile.getApprovalNotice())
                .rejectionReason(profile.getRejectionReason())
                .reapplyCount(profile.getReapplyCount() == null ? 0 : profile.getReapplyCount())
                .approvedAt(profile.getApprovedAt())
                .partnerConfirmedAt(profile.getPartnerConfirmedAt())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
