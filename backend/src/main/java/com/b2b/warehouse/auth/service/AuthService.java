package com.b2b.warehouse.auth.service;

import com.b2b.warehouse.auth.dto.AuthTokenResponseDto;
import com.b2b.warehouse.auth.dto.B2bRegisterRequestDto;
import com.b2b.warehouse.auth.dto.ChangePasswordRequestDto;
import com.b2b.warehouse.auth.dto.LoginRequestDto;
import com.b2b.warehouse.auth.dto.RefreshTokenRequestDto;
import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.auth.repository.UserRepository;
import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.auth.security.JwtTokenProvider;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.partner.entity.B2bPartnerProfileEntity;
import com.b2b.warehouse.partner.repository.B2bPartnerProfileRepository;
import com.b2b.warehouse.rbac.entity.PermissionEntity;
import com.b2b.warehouse.rbac.entity.RoleCode;
import com.b2b.warehouse.rbac.entity.RoleEntity;
import com.b2b.warehouse.rbac.repository.RoleRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final B2bPartnerProfileRepository partnerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStoreService refreshTokenStoreService;

    /**
     * Stage 1 B2B Partner Registration:
     * Immediately provisions an account in {@code PENDING_ADMIN} status and returns JWT tokens
     * so the partner can log in to view the 3-Step Onboarding Status Tracker.
     */
    @Transactional
    public AuthTokenResponseDto registerB2bPartner(B2bRegisterRequestDto request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedTaxCode = request.taxCode().trim();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (partnerProfileRepository.existsByTaxCode(normalizedTaxCode)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TAX_CODE);
        }

        RoleEntity partnerRole = roleRepository.findByCode(RoleCode.B2B_PARTNER.name())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "B2B_PARTNER role is not configured"));

        UserEntity user = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.contactPersonName().trim())
                .phone(request.contactPhone().trim())
                .role(partnerRole)
                .accountStatus(AccountStatus.PENDING_ADMIN)
                .tokenVersion(1)
                .lastLoginAt(Instant.now())
                .build();
        user = userRepository.save(user);

        B2bPartnerProfileEntity profile = B2bPartnerProfileEntity.builder()
                .user(user)
                .companyName(request.companyName().trim())
                .taxCode(normalizedTaxCode)
                .businessType(request.businessType())
                .representativeName(request.representativeName().trim())
                .contactPersonName(request.contactPersonName().trim())
                .contactPhone(request.contactPhone().trim())
                .businessLicenseUrl(request.businessLicenseUrl().trim())
                .postalCode(request.postalCode())
                .addressLine(request.addressLine().trim())
                .addressDetail(request.addressDetail())
                .shippingPostalCode(request.postalCode())
                .shippingAddressLine(request.addressLine().trim())
                .shippingAddressDetail(request.addressDetail())
                .reapplyCount(0)
                .build();
        partnerProfileRepository.save(profile);

        return createNewSessionTokens(user);
    }

    /**
     * Authenticates user and issues a Dual-Token pair with a new Redis Refresh Token family.
     */
    @Transactional
    public AuthTokenResponseDto login(LoginRequestDto request) {
        UserEntity user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Your account has been suspended. Please contact B2B Support.");
        }

        user.setLastLoginAt(Instant.now());
        return createNewSessionTokens(user);
    }

    /**
     * Rotates a single-use Refresh Token and detects token family theft.
     */
    @Transactional(readOnly = true)
    public AuthTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        Claims claims = jwtTokenProvider.parseAndValidateClaims(request.refreshToken());
        String tokenType = claims.get("typ", String.class);
        if (!JwtTokenProvider.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Provided token is not a REFRESH token");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        String familyId = claims.get("fam", String.class);
        String presentedJti = claims.getId();
        Integer tokenVersion = claims.get("ver", Integer.class);

        UserEntity user = userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Account no longer exists"));

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            refreshTokenStoreService.revokeAllUserSessions(userId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "Account is suspended");
        }

        if (tokenVersion == null || !tokenVersion.equals(user.getTokenVersion())) {
            refreshTokenStoreService.revokeFamily(userId, familyId);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Session invalidated due to security state change. Please log in again.");
        }

        String nextJti = UUID.randomUUID().toString();
        refreshTokenStoreService.verifyAndRotate(userId, familyId, presentedJti, nextJti);

        return buildTokenResponse(user, familyId, nextJti);
    }

    /**
     * Revokes the current Refresh Token family upon logout.
     */
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parseAndValidateClaims(refreshToken);
            UUID userId = UUID.fromString(claims.getSubject());
            String familyId = claims.get("fam", String.class);
            refreshTokenStoreService.revokeFamily(userId, familyId);
        } catch (BusinessException ignored) {
            // Token already expired or invalid; idempotent logout
        }
    }

    /**
     * Updates user password, increments tokenVersion, and revokes all active Redis sessions.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDto request) {
        UserEntity user = userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User account not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.incrementTokenVersion();
        userRepository.save(user);

        refreshTokenStoreService.revokeAllUserSessions(userId);
    }

    /**
     * Issues a fresh token pair when a B2B Partner transitions to {@code ACTIVE} in Stage 3.
     */
    public AuthTokenResponseDto createNewSessionTokens(UserEntity user) {
        String familyId = UUID.randomUUID().toString();
        String initialJti = UUID.randomUUID().toString();
        refreshTokenStoreService.createFamily(user.getId(), familyId, initialJti);
        return buildTokenResponse(user, familyId, initialJti);
    }

    private AuthTokenResponseDto buildTokenResponse(UserEntity user, String familyId, String jti) {
        List<String> permissions = user.getRole().getPermissions().stream()
                .map(PermissionEntity::getCode)
                .sorted()
                .toList();

        AuthenticatedUser principal = AuthenticatedUser.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .roleCode(user.getRole().getCode())
                .accountStatus(user.getAccountStatus())
                .tokenVersion(user.getTokenVersion())
                .permissions(permissions)
                .build();

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                familyId,
                jti,
                user.getTokenVersion()
        );

        return AuthTokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtTokenProvider.getAccessExpirationSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getCode())
                .accountStatus(user.getAccountStatus())
                .onboardingStep(resolveOnboardingStep(user.getAccountStatus()))
                .permissions(permissions)
                .build();
    }

    public static int resolveOnboardingStep(AccountStatus status) {
        if (status == null) {
            return 1;
        }
        return switch (status) {
            case PENDING_ADMIN, REJECTED, SUSPENDED -> 1;
            case ADMIN_APPROVED -> 2;
            case ACTIVE -> 3;
        };
    }
}
