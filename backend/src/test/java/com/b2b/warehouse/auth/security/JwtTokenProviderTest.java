package com.b2b.warehouse.auth.security;

import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "YjJiLXdhcmVob3VzZS1zdXBlci1zZWNyZXQtand0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seS0yMDI2LWhpZ2gtdHJhZmZpYw==";
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 900L, 604800L);
    }

    @Test
    @DisplayName("Should issue and parse stateless Access Token with role, status, and RBAC permissions")
    void shouldGenerateAndExtractAccessToken() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser principal = AuthenticatedUser.builder()
                .userId(userId)
                .email("partner@umego.vn")
                .roleCode("B2B_PARTNER")
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(2)
                .permissions(List.of("order:read", "order:write"))
                .build();

        String token = jwtTokenProvider.generateAccessToken(principal);
        AuthenticatedUser extracted = jwtTokenProvider.extractAuthenticatedUser(token);

        assertThat(extracted.getUserId()).isEqualTo(userId);
        assertThat(extracted.getEmail()).isEqualTo("partner@umego.vn");
        assertThat(extracted.getRoleCode()).isEqualTo("B2B_PARTNER");
        assertThat(extracted.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(extracted.getTokenVersion()).isEqualTo(2);
        assertThat(extracted.getPermissions()).containsExactly("order:read", "order:write");
    }

    @Test
    @DisplayName("Should issue Refresh Token with familyId and jti and reject it if used as an Access Token")
    void shouldGenerateRefreshTokenAndRejectAsAccessToken() {
        UUID userId = UUID.randomUUID();
        String familyId = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, familyId, jti, 1);
        Claims claims = jwtTokenProvider.parseAndValidateClaims(refreshToken);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("fam", String.class)).isEqualTo(familyId);
        assertThat(claims.getId()).isEqualTo(jti);

        assertThatThrownBy(() -> jwtTokenProvider.extractAuthenticatedUser(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should throw TOKEN_EXPIRED when token TTL has elapsed")
    void shouldThrowTokenExpiredWhenTtlElapsed() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -10L, 604800L);
        AuthenticatedUser principal = AuthenticatedUser.builder()
                .userId(UUID.randomUUID())
                .email("expired@umego.vn")
                .roleCode("ADMIN")
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(1)
                .permissions(List.of())
                .build();

        String expiredToken = expiredProvider.generateAccessToken(principal);

        assertThatThrownBy(() -> jwtTokenProvider.extractAuthenticatedUser(expiredToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }
}
