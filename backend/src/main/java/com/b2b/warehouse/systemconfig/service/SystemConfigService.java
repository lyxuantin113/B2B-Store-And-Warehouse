package com.b2b.warehouse.systemconfig.service;

import com.b2b.warehouse.common.util.AesGcmCryptoUtil;
import com.b2b.warehouse.common.util.HtmlSanitizerUtil;
import com.b2b.warehouse.systemconfig.dto.PolicyResponseDto;
import com.b2b.warehouse.systemconfig.dto.SystemConfigResponseDto;
import com.b2b.warehouse.systemconfig.dto.UpdatePolicyRequestDto;
import com.b2b.warehouse.systemconfig.dto.UpsertSystemConfigRequestDto;
import com.b2b.warehouse.systemconfig.entity.SystemConfigEntity;
import com.b2b.warehouse.systemconfig.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages dynamic third-party API configurations (VNPay, MoMo, VietQR, GHN, GHTK, AhaMove, Google Maps)
 * and the B2B Wholesale &amp; Sample Policy CMS page.
 *
 * <p>Security design:
 * <ul>
 *   <li>Sensitive API keys ({@code isEncrypted = true}) are encrypted at rest using AES-256-GCM.</li>
 *   <li>When returned to the Admin UI, encrypted values are masked (e.g., {@code ********a9f2}) so secrets never leak to the browser.</li>
 *   <li>Rich-text policy HTML ({@code B2B_POLICY_HTML}) is sanitized via OWASP {@link HtmlSanitizerUtil} to prevent Stored XSS.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    public static final String POLICY_CONFIG_KEY = "B2B_POLICY_HTML";
    public static final String POLICY_GROUP = "POLICY";

    private static final String DEFAULT_POLICY_HTML =
            "<h3>B2B Wholesale &amp; Sample Policy</h3>" +
            "<p>Welcome to our B2B Partner Portal. Wholesale unit prices scale dynamically by quantity bracket per product. " +
            "Free samples cost 0 VND for merchandise; partners only pay the carrier shipping fee.</p>";

    private final SystemConfigRepository systemConfigRepository;
    private final AesGcmCryptoUtil aesGcmCryptoUtil;
    private final HtmlSanitizerUtil htmlSanitizerUtil;

    @Transactional(readOnly = true)
    public List<SystemConfigResponseDto> listConfigsForAdmin(String configGroup) {
        List<SystemConfigEntity> entities = (configGroup == null || configGroup.isBlank())
                ? systemConfigRepository.findAllByOrderByConfigGroupAscConfigKeyAsc()
                : systemConfigRepository.findAllByConfigGroupIgnoreCaseOrderByConfigKeyAsc(configGroup.trim());

        return entities.stream()
                .map(this::toAdminDtoWithMasking)
                .toList();
    }

    @Transactional
    public SystemConfigResponseDto upsertConfig(UUID adminUserId, UpsertSystemConfigRequestDto request) {
        String normalizedKey = request.configKey().trim().toUpperCase();
        String normalizedGroup = request.configGroup().trim().toUpperCase();
        boolean encrypted = Boolean.TRUE.equals(request.isEncrypted());

        String valueToStore = request.configValue();
        if (POLICY_CONFIG_KEY.equals(normalizedKey) || POLICY_GROUP.equals(normalizedGroup)) {
            valueToStore = htmlSanitizerUtil.sanitizeRichText(valueToStore);
            encrypted = false;
        } else if (encrypted) {
            valueToStore = aesGcmCryptoUtil.encrypt(valueToStore.trim());
        }

        SystemConfigEntity entity = systemConfigRepository.findById(normalizedKey)
                .orElseGet(() -> SystemConfigEntity.builder()
                        .configKey(normalizedKey)
                        .build());

        entity.setConfigGroup(normalizedGroup);
        entity.setConfigValue(valueToStore);
        entity.setEncrypted(encrypted);
        entity.setUpdatedBy(adminUserId);
        entity.setUpdatedAt(Instant.now());

        return toAdminDtoWithMasking(systemConfigRepository.save(entity));
    }

    /**
     * Internal service lookup that automatically decrypts AES-256-GCM values when {@code isEncrypted = true}.
     */
    @Transactional(readOnly = true)
    public String getDecryptedValueOrDefault(String configKey, String defaultValue) {
        Optional<SystemConfigEntity> opt = systemConfigRepository.findById(configKey);
        if (opt.isEmpty()) {
            return defaultValue;
        }
        SystemConfigEntity entity = opt.get();
        if (entity.isEncrypted()) {
            return aesGcmCryptoUtil.decrypt(entity.getConfigValue());
        }
        return entity.getConfigValue();
    }

    @Transactional(readOnly = true)
    public PolicyResponseDto getB2bPolicy() {
        return systemConfigRepository.findById(POLICY_CONFIG_KEY)
                .map(entity -> PolicyResponseDto.builder()
                        .policyHtml(entity.getConfigValue())
                        .updatedAt(entity.getUpdatedAt())
                        .build())
                .orElseGet(() -> PolicyResponseDto.builder()
                        .policyHtml(DEFAULT_POLICY_HTML)
                        .updatedAt(Instant.now())
                        .build());
    }

    @Transactional
    public PolicyResponseDto updateB2bPolicy(UUID adminUserId, UpdatePolicyRequestDto request) {
        String sanitizedHtml = htmlSanitizerUtil.sanitizeRichText(request.policyHtml());
        SystemConfigEntity entity = systemConfigRepository.findById(POLICY_CONFIG_KEY)
                .orElseGet(() -> SystemConfigEntity.builder()
                        .configKey(POLICY_CONFIG_KEY)
                        .configGroup(POLICY_GROUP)
                        .isEncrypted(false)
                        .build());

        entity.setConfigGroup(POLICY_GROUP);
        entity.setConfigValue(sanitizedHtml);
        entity.setEncrypted(false);
        entity.setUpdatedBy(adminUserId);
        entity.setUpdatedAt(Instant.now());

        SystemConfigEntity saved = systemConfigRepository.save(entity);
        return PolicyResponseDto.builder()
                .policyHtml(saved.getConfigValue())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    private SystemConfigResponseDto toAdminDtoWithMasking(SystemConfigEntity entity) {
        String displayValue = entity.getConfigValue();
        if (entity.isEncrypted()) {
            try {
                String plain = aesGcmCryptoUtil.decrypt(entity.getConfigValue());
                displayValue = maskSecret(plain);
            } catch (Exception ignored) {
                displayValue = "****************";
            }
        }
        return SystemConfigResponseDto.builder()
                .configKey(entity.getConfigKey())
                .configGroup(entity.getConfigGroup())
                .configValue(displayValue)
                .isEncrypted(entity.isEncrypted())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 4) {
            return "********";
        }
        return "************" + secret.substring(secret.length() - 4);
    }
}
