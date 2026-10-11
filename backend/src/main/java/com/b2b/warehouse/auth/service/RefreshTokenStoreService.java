package com.b2b.warehouse.auth.service;

import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Manages Refresh Token families in Redis to support:
 * <ol>
 *   <li><b>Single-Use Refresh Token Rotation</b>: Each refresh replaces the active {@code jti} in {@code rt:{familyId}}.</li>
 *   <li><b>Automatic Reuse (Theft) Detection</b>: If a previously rotated {@code jti} is re-submitted for an existing
 *       {@code familyId}, all active sessions of that user are immediately wiped from Redis and {@code ERR_TOKEN_REUSE_DETECTED} is thrown.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenStoreService {

    private static final String RT_FAMILY_KEY_PREFIX = "rt:";
    private static final String USER_FAMILIES_SET_PREFIX = "user_rt_families:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.security.jwt.refresh-expiration-seconds:604800}")
    private long refreshExpirationSeconds;

    /**
     * Registers a new Refresh Token family upon login or B2B registration.
     */
    public void createFamily(UUID userId, String familyId, String initialJti) {
        Duration ttl = Duration.ofSeconds(refreshExpirationSeconds);
        String familyKey = RT_FAMILY_KEY_PREFIX + familyId;
        String userSetKey = USER_FAMILIES_SET_PREFIX + userId;

        stringRedisTemplate.opsForValue().set(familyKey, userId + ":" + initialJti, ttl);
        stringRedisTemplate.opsForSet().add(userSetKey, familyId);
        stringRedisTemplate.expire(userSetKey, ttl);
    }

    /**
     * Verifies that {@code presentedJti} matches the current active JTI for {@code familyId},
     * and rotates it to {@code nextJti}. If a mismatch is detected, revokes all sessions for the user.
     */
    public void verifyAndRotate(UUID userId, String familyId, String presentedJti, String nextJti) {
        String familyKey = RT_FAMILY_KEY_PREFIX + familyId;
        String storedValue = stringRedisTemplate.opsForValue().get(familyKey);

        if (storedValue == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Refresh token session has expired or been logged out");
        }

        String expectedValue = userId + ":" + presentedJti;
        if (!expectedValue.equals(storedValue)) {
            log.error("SECURITY ALERT: Refresh token reuse detected for userId={}, familyId={}!", userId, familyId);
            revokeAllUserSessions(userId);
            throw new BusinessException(ErrorCode.TOKEN_REUSE_DETECTED);
        }

        Duration ttl = Duration.ofSeconds(refreshExpirationSeconds);
        stringRedisTemplate.opsForValue().set(familyKey, userId + ":" + nextJti, ttl);
    }

    /**
     * Revokes a single refresh token family upon logout.
     */
    public void revokeFamily(UUID userId, String familyId) {
        stringRedisTemplate.delete(RT_FAMILY_KEY_PREFIX + familyId);
        if (userId != null) {
            stringRedisTemplate.opsForSet().remove(USER_FAMILIES_SET_PREFIX + userId, familyId);
        }
    }

    /**
     * Wipes all active refresh token families for a user (triggered on password change, account suspension, or token theft detection).
     */
    public void revokeAllUserSessions(UUID userId) {
        String userSetKey = USER_FAMILIES_SET_PREFIX + userId;
        Set<String> familyIds = stringRedisTemplate.opsForSet().members(userSetKey);
        if (familyIds != null && !familyIds.isEmpty()) {
            for (String familyId : familyIds) {
                stringRedisTemplate.delete(RT_FAMILY_KEY_PREFIX + familyId);
            }
        }
        stringRedisTemplate.delete(userSetKey);
    }
}
