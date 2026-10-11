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
import com.b2b.warehouse.partner.dto.AdminPartnerStatusUpdateRequestDto;
import com.b2b.warehouse.partner.dto.B2bOnboardingStatusDto;
import com.b2b.warehouse.partner.dto.B2bReapplyRequestDto;
import com.b2b.warehouse.partner.dto.PartnerProfileResponseDto;
import com.b2b.warehouse.partner.dto.PartnerSelfUpdateProfileRequestDto;
import com.b2b.warehouse.partner.entity.B2bPartnerProfileEntity;
import com.b2b.warehouse.partner.repository.B2bPartnerProfileRepository;
import com.b2b.warehouse.rbac.entity.RoleCode;
import com.b2b.warehouse.rbac.entity.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class B2bPartnerServiceTest {

    @Mock
    private B2bPartnerProfileRepository partnerProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;
    @Mock
    private RefreshTokenStoreService refreshTokenStoreService;

    private B2bPartnerService partnerService;

    @BeforeEach
    void setUp() {
        partnerService = new B2bPartnerService(
                partnerProfileRepository,
                userRepository,
                authService,
                refreshTokenStoreService,
                new HtmlSanitizerUtil()
        );
    }

    @Test
    @DisplayName("Stage 2 -> Stage 3: Admin approves with sanitized notice, then Partner confirms to unlock ACTIVE portal")
    void shouldTransitionFromPendingToAdminApprovedToActive() {
        UUID partnerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        RoleEntity partnerRole = RoleEntity.builder().id((short) 4).code(RoleCode.B2B_PARTNER.name()).build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("partner@umego.vn")
                .role(partnerRole)
                .accountStatus(AccountStatus.PENDING_ADMIN)
                .build();
        B2bPartnerProfileEntity profile = B2bPartnerProfileEntity.builder()
                .id(partnerId)
                .user(user)
                .companyName("Umego Partner Co")
                .taxCode("0312345678")
                .build();

        when(partnerProfileRepository.findWithUserById(partnerId)).thenReturn(Optional.of(profile));

        // Step A: Admin approves (Stage 1 -> Stage 2 ADMIN_APPROVED) with XSS payload in notice
        AdminPartnerStatusUpdateRequestDto approveReq = AdminPartnerStatusUpdateRequestDto.builder()
                .targetStatus(AccountStatus.ADMIN_APPROVED)
                .approvalNotice("<p>Verified tax code.</p><script>alert('xss')</script>")
                .build();

        PartnerProfileResponseDto stage2Dto = partnerService.updatePartnerStatusByAdmin(partnerId, adminId, approveReq);

        assertThat(stage2Dto.accountStatus()).isEqualTo(AccountStatus.ADMIN_APPROVED);
        assertThat(stage2Dto.approvalNotice())
                .contains("<p>Verified tax code.</p>")
                .doesNotContain("<script>");

        // Step B: Partner clicks Confirm (Stage 2 ADMIN_APPROVED -> Stage 3 ACTIVE)
        when(partnerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(authService.createNewSessionTokens(user)).thenReturn(
                AuthTokenResponseDto.builder()
                        .accountStatus(AccountStatus.ACTIVE)
                        .onboardingStep(3)
                        .accessToken("new-active-jwt")
                        .build()
        );

        AuthTokenResponseDto activeTokens = partnerService.confirmOnboardingAndActivate(userId);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(profile.getPartnerConfirmedAt()).isNotNull();
        assertThat(activeTokens.onboardingStep()).isEqualTo(3);
    }

    @Test
    @DisplayName("Stage 1 Re-apply: Rejected partner can edit and resubmit to re-enter PENDING_ADMIN with incremented reapplyCount")
    void shouldAllowRejectedPartnerToReapply() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("rejected@umego.vn")
                .accountStatus(AccountStatus.REJECTED)
                .build();
        B2bPartnerProfileEntity profile = B2bPartnerProfileEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .companyName("Old Name")
                .taxCode("0311111111")
                .rejectionReason("Blurred business license scan")
                .reapplyCount(0)
                .build();

        when(partnerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(partnerProfileRepository.existsByTaxCodeAndUserIdNot("0311111111", userId)).thenReturn(false);

        B2bReapplyRequestDto reapplyRequest = B2bReapplyRequestDto.builder()
                .companyName("Updated Legal Name Co., Ltd")
                .taxCode("0311111111")
                .businessType("Wholesale")
                .representativeName("Le Van C")
                .contactPersonName("Le Van C")
                .contactPhone("0987654321")
                .businessLicenseUrl("https://minio.local/b2b-licenses/clear_scan.pdf")
                .addressLine("456 Le Loi, District 1")
                .build();

        B2bOnboardingStatusDto result = partnerService.reapply(userId, reapplyRequest);

        assertThat(result.accountStatus()).isEqualTo(AccountStatus.PENDING_ADMIN);
        assertThat(result.currentStep()).isEqualTo(1);
        assertThat(result.reapplyCount()).isEqualTo(1);
        assertThat(result.rejectionReason()).isNull();
        assertThat(result.companyName()).isEqualTo("Updated Legal Name Co., Ltd");
    }

    @Test
    @DisplayName("Tab 8 Profile Update: Should update operational shipping/contact fields while keeping legal fields immutable")
    void shouldUpdateOperationalFieldsWhilePreservingImmutableLegalFields() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("active@umego.vn")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        B2bPartnerProfileEntity profile = B2bPartnerProfileEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .companyName("Immutable Legal Company LLC")
                .taxCode("0399999999")
                .representativeName("Immutable CEO")
                .contactPersonName("Old Contact")
                .contactPhone("0900000001")
                .shippingAddressLine("Old Warehouse")
                .build();

        when(partnerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        PartnerSelfUpdateProfileRequestDto updateReq = PartnerSelfUpdateProfileRequestDto.builder()
                .contactPersonName("New Purchasing Manager")
                .contactPhone("0912345678")
                .shippingPostalCode("700000")
                .shippingAddressLine("789 Nguyen Van Linh, District 7, HCMC")
                .shippingAddressDetail("Dock B2")
                .build();

        PartnerProfileResponseDto updated = partnerService.updateMyOperationalProfile(userId, updateReq);

        // Legal fields remain unchanged
        assertThat(updated.companyName()).isEqualTo("Immutable Legal Company LLC");
        assertThat(updated.taxCode()).isEqualTo("0399999999");
        assertThat(updated.representativeName()).isEqualTo("Immutable CEO");
        // Operational fields updated
        assertThat(updated.contactPersonName()).isEqualTo("New Purchasing Manager");
        assertThat(updated.shippingAddressLine()).isEqualTo("789 Nguyen Van Linh, District 7, HCMC");
    }

    @Test
    @DisplayName("Should prevent Partner from confirming onboarding if status is still PENDING_ADMIN")
    void shouldRejectConfirmWhenStillPendingAdmin() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .accountStatus(AccountStatus.PENDING_ADMIN)
                .build();
        B2bPartnerProfileEntity profile = B2bPartnerProfileEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .build();

        when(partnerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> partnerService.confirmOnboardingAndActivate(userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ONBOARDING_STATE);
    }

    @Test
    @DisplayName("Admin suspending a partner should increment tokenVersion and revoke all Redis refresh sessions")
    void shouldRevokeSessionsWhenAdminSuspendsPartner() {
        UUID partnerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(1)
                .build();
        B2bPartnerProfileEntity profile = B2bPartnerProfileEntity.builder()
                .id(partnerId)
                .user(user)
                .build();

        when(partnerProfileRepository.findWithUserById(partnerId)).thenReturn(Optional.of(profile));

        partnerService.updatePartnerStatusByAdmin(
                partnerId,
                UUID.randomUUID(),
                AdminPartnerStatusUpdateRequestDto.builder().targetStatus(AccountStatus.SUSPENDED).build()
        );

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(user.getTokenVersion()).isEqualTo(2);
        verify(refreshTokenStoreService).revokeAllUserSessions(userId);
    }
}
