package com.b2b.warehouse.auth.service;

import com.b2b.warehouse.auth.dto.AuthTokenResponseDto;
import com.b2b.warehouse.auth.dto.B2bRegisterRequestDto;
import com.b2b.warehouse.auth.dto.ChangePasswordRequestDto;
import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.auth.repository.UserRepository;
import com.b2b.warehouse.auth.security.JwtTokenProvider;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.partner.repository.B2bPartnerProfileRepository;
import com.b2b.warehouse.rbac.entity.PermissionEntity;
import com.b2b.warehouse.rbac.entity.RoleCode;
import com.b2b.warehouse.rbac.entity.RoleEntity;
import com.b2b.warehouse.rbac.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private B2bPartnerProfileRepository partnerProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenStoreService refreshTokenStoreService;

    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        String secret = "YjJiLXdhcmVob3VzZS1zdXBlci1zZWNyZXQtand0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seS0yMDI2LWhpZ2gtdHJhZmZpYw==";
        jwtTokenProvider = new JwtTokenProvider(secret, 900L, 604800L);
        authService = new AuthService(
                userRepository,
                roleRepository,
                partnerProfileRepository,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenStoreService
        );
    }

    @Test
    @DisplayName("Stage 1 B2B Registration should create PENDING_ADMIN account at onboardingStep=1 and issue tracking tokens")
    void shouldRegisterB2bPartnerInStage1PendingAdmin() {
        B2bRegisterRequestDto request = B2bRegisterRequestDto.builder()
                .email("Wholesale@Umego.vn")
                .password("SecurePass#2026")
                .companyName("Umego Vietnam Co., Ltd")
                .taxCode("0312345678")
                .businessType("Cosmetics Distributor")
                .representativeName("Nguyen Van A")
                .contactPersonName("Tran Thi B")
                .contactPhone("0901234567")
                .businessLicenseUrl("https://minio.local/b2b-licenses/0312345678.pdf")
                .postalCode("700000")
                .addressLine("123 Nguyen Hue, District 1, HCMC")
                .build();

        RoleEntity partnerRole = RoleEntity.builder()
                .id((short) 4)
                .code(RoleCode.B2B_PARTNER.name())
                .name("B2B Partner")
                .permissions(Set.of(new PermissionEntity((short) 7, "order:read", "ORDER", "")))
                .build();

        when(userRepository.existsByEmailIgnoreCase("wholesale@umego.vn")).thenReturn(false);
        when(partnerProfileRepository.existsByTaxCode("0312345678")).thenReturn(false);
        when(roleRepository.findByCode("B2B_PARTNER")).thenReturn(Optional.of(partnerRole));
        when(passwordEncoder.encode("SecurePass#2026")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AuthTokenResponseDto response = authService.registerB2bPartner(request);

        assertThat(response.accountStatus()).isEqualTo(AccountStatus.PENDING_ADMIN);
        assertThat(response.onboardingStep()).isEqualTo(1);
        assertThat(response.role()).isEqualTo("B2B_PARTNER");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        verify(partnerProfileRepository).save(any());
    }

    @Test
    @DisplayName("Should reject B2B registration when tax code is already registered")
    void shouldRejectDuplicateTaxCode() {
        B2bRegisterRequestDto request = B2bRegisterRequestDto.builder()
                .email("new@umego.vn")
                .password("SecurePass#2026")
                .companyName("Dup Co")
                .taxCode("0312345678")
                .representativeName("Rep")
                .contactPersonName("Contact")
                .contactPhone("0901234567")
                .businessLicenseUrl("https://minio.local/lic.pdf")
                .addressLine("HCMC")
                .build();

        when(userRepository.existsByEmailIgnoreCase("new@umego.vn")).thenReturn(false);
        when(partnerProfileRepository.existsByTaxCode("0312345678")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerB2bPartner(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_TAX_CODE);
    }

    @Test
    @DisplayName("Should increment tokenVersion and revoke all Redis sessions when changing password")
    void shouldIncrementTokenVersionAndRevokeSessionsOnPasswordChange() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("partner@umego.vn")
                .passwordHash("$2a$12$oldHash")
                .tokenVersion(1)
                .build();

        when(userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass#123", "$2a$12$oldHash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass#456")).thenReturn("$2a$12$newHash");

        authService.changePassword(userId, new ChangePasswordRequestDto("OldPass#123", "NewPass#456"));

        assertThat(user.getTokenVersion()).isEqualTo(2);
        assertThat(user.getPasswordHash()).isEqualTo("$2a$12$newHash");
        verify(refreshTokenStoreService).revokeAllUserSessions(userId);
    }
}
