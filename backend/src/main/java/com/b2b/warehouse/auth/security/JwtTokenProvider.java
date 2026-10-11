package com.b2b.warehouse.auth.security;

import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and verifies JWT Access Tokens (15m TTL) and Refresh Tokens (7d TTL).
 *
 * <ul>
 *   <li>Access Token claims: {@code sub} (userId), {@code email}, {@code role}, {@code status}, {@code ver}, {@code perms}, {@code typ=ACCESS}</li>
 *   <li>Refresh Token claims: {@code sub} (userId), {@code fam} (familyId), {@code jti} (tokenId), {@code ver}, {@code typ=REFRESH}</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final SecretKey signingKey;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String base64Secret,
            @Value("${app.security.jwt.access-expiration-seconds:900}") long accessExpirationSeconds,
            @Value("${app.security.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    public String generateAccessToken(AuthenticatedUser principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessExpirationSeconds);

        return Jwts.builder()
                .subject(principal.getUserId().toString())
                .claim("typ", TOKEN_TYPE_ACCESS)
                .claim("email", principal.getEmail())
                .claim("role", principal.getRoleCode())
                .claim("status", principal.getAccountStatus().name())
                .claim("ver", principal.getTokenVersion())
                .claim("perms", principal.getPermissions() == null ? List.of() : principal.getPermissions())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String familyId, String jti, int tokenVersion) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshExpirationSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .id(jti)
                .claim("typ", TOKEN_TYPE_REFRESH)
                .claim("fam", familyId)
                .claim("ver", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidateClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid authentication token");
        }
    }

    @SuppressWarnings("unchecked")
    public AuthenticatedUser extractAuthenticatedUser(String accessToken) {
        Claims claims = parseAndValidateClaims(accessToken);
        String tokenType = claims.get("typ", String.class);
        if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Expected an ACCESS token");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String roleCode = claims.get("role", String.class);
        AccountStatus status = AccountStatus.valueOf(claims.get("status", String.class));
        Integer tokenVersion = claims.get("ver", Integer.class);
        List<String> perms = claims.get("perms", List.class);

        return AuthenticatedUser.builder()
                .userId(userId)
                .email(email)
                .roleCode(roleCode)
                .accountStatus(status)
                .tokenVersion(tokenVersion == null ? 1 : tokenVersion)
                .permissions(perms == null ? List.of() : perms)
                .build();
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationSeconds;
    }
}
