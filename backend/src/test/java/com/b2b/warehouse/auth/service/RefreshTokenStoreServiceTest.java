package com.b2b.warehouse.auth.service;

import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private RefreshTokenStoreService refreshTokenStoreService;

    @BeforeEach
    void setUp() {
        refreshTokenStoreService = new RefreshTokenStoreService(stringRedisTemplate);
        ReflectionTestUtils.setField(refreshTokenStoreService, "refreshExpirationSeconds", 604800L);
    }

    @Test
    @DisplayName("Should rotate active JTI when presented JTI matches Redis family record")
    void shouldRotateJtiWhenValid() {
        UUID userId = UUID.randomUUID();
        String familyId = "fam-100";
        String oldJti = "jti-1";
        String newJti = "jti-2";

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rt:" + familyId)).thenReturn(userId + ":" + oldJti);

        refreshTokenStoreService.verifyAndRotate(userId, familyId, oldJti, newJti);

        verify(valueOperations).set(
                eq("rt:" + familyId),
                eq(userId + ":" + newJti),
                eq(Duration.ofSeconds(604800L))
        );
    }

    @Test
    @DisplayName("Should detect Refresh Token reuse (theft), wipe all user sessions, and throw TOKEN_REUSE_DETECTED")
    void shouldDetectTokenReuseAndRevokeAllUserSessions() {
        UUID userId = UUID.randomUUID();
        String familyId = "fam-stolen";
        String alreadyRotatedJti = "jti-old-reused";
        String currentActiveJtiInRedis = "jti-current";

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get("rt:" + familyId)).thenReturn(userId + ":" + currentActiveJtiInRedis);
        when(setOperations.members("user_rt_families:" + userId)).thenReturn(Set.of(familyId, "fam-mobile-device"));

        assertThatThrownBy(() -> refreshTokenStoreService.verifyAndRotate(userId, familyId, alreadyRotatedJti, "jti-next"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_REUSE_DETECTED);

        verify(stringRedisTemplate).delete("rt:" + familyId);
        verify(stringRedisTemplate).delete("rt:fam-mobile-device");
        verify(stringRedisTemplate).delete("user_rt_families:" + userId);
    }
}
