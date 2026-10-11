package com.b2b.warehouse.branch.service;

import com.b2b.warehouse.branch.dto.NearbyBranchResponseDto;
import com.b2b.warehouse.branch.dto.StandardizedAddressDto;
import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.branch.repository.BranchRepository;
import com.b2b.warehouse.common.util.AesGcmCryptoUtil;
import com.b2b.warehouse.common.util.HtmlSanitizerUtil;
import com.b2b.warehouse.systemconfig.dto.PolicyResponseDto;
import com.b2b.warehouse.systemconfig.dto.SystemConfigResponseDto;
import com.b2b.warehouse.systemconfig.dto.UpdatePolicyRequestDto;
import com.b2b.warehouse.systemconfig.dto.UpsertSystemConfigRequestDto;
import com.b2b.warehouse.systemconfig.entity.SystemConfigEntity;
import com.b2b.warehouse.systemconfig.repository.SystemConfigRepository;
import com.b2b.warehouse.systemconfig.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreLocatorAndSystemConfigTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private AesGcmCryptoUtil aesGcmCryptoUtil;
    private HtmlSanitizerUtil htmlSanitizerUtil;
    private SystemConfigService systemConfigService;
    private GoogleMapsGeocodingService googleMapsGeocodingService;
    private BranchService branchService;

    @BeforeEach
    void setUp() {
        String base64Key = Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
        );
        aesGcmCryptoUtil = new AesGcmCryptoUtil(base64Key);
        htmlSanitizerUtil = new HtmlSanitizerUtil();
        systemConfigService = new SystemConfigService(systemConfigRepository, aesGcmCryptoUtil, htmlSanitizerUtil);
        googleMapsGeocodingService = new GoogleMapsGeocodingService(systemConfigService);
        branchService = new BranchService(branchRepository, googleMapsGeocodingService);
    }

    @Test
    @DisplayName("GoogleMapsGeocodingService standardizes Vietnamese addresses and resolves GPS coordinates")
    void shouldStandardizeVietnameseAddressAndResolveCoordinates() {
        when(systemConfigRepository.findById("GOOGLE_MAPS_API_KEY")).thenReturn(Optional.empty());

        StandardizedAddressDto dto = googleMapsGeocodingService.standardizeAddress("123 Nguyễn Văn Linh, Quận 7");

        assertThat(dto.formattedAddress()).contains("Quận 7, TP. Hồ Chí Minh, Việt Nam");
        assertThat(dto.latitude()).isEqualTo(10.729560);
        assertThat(dto.longitude()).isEqualTo(106.721750);
        assertThat(dto.postalCode()).isEqualTo("700000");
        assertThat(dto.placeId()).startsWith("ChIJ_");
    }

    @Test
    @DisplayName("BranchService.searchNearestBranches geocodes address, calculates Haversine distance, and sorts by pinned + distance")
    void shouldSearchNearestBranchesByAddressAndRadius() {
        when(systemConfigRepository.findById("GOOGLE_MAPS_API_KEY")).thenReturn(Optional.empty());

        BranchEntity nearStandardBranch = BranchEntity.builder()
                .id(UUID.randomUUID())
                .code("HCM-Q1-NEAR")
                .name("District 1 Flagship Store")
                .addressLine("10 Nguyễn Huệ, Quận 1")
                .latitude(10.775000)
                .longitude(106.702000)
                .isPinned(false)
                .isActive(true)
                .build();

        BranchEntity pinnedBranch = BranchEntity.builder()
                .id(UUID.randomUUID())
                .code("HCM-Q3-PINNED")
                .name("District 3 Central Warehouse")
                .addressLine("200 Võ Văn Tần, Quận 3")
                .latitude(10.785000)
                .longitude(106.690000)
                .isPinned(true)
                .isActive(true)
                .build();

        BranchEntity farHanoiBranch = BranchEntity.builder()
                .id(UUID.randomUUID())
                .code("HN-CAUGIAY")
                .name("Hanoi Warehouse")
                .addressLine("Cầu Giấy, Hà Nội")
                .latitude(21.028511)
                .longitude(105.804817)
                .isPinned(false)
                .isActive(true)
                .build();

        when(branchRepository.findNearbyBranchesWithinRadiusMeters(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(nearStandardBranch, pinnedBranch, farHanoiBranch));

        List<NearbyBranchResponseDto> results = branchService.searchNearestBranches(
                null,
                null,
                "Bến Nghé, Quận 1",
                10.0,
                null,
                null
        );

        // Far Hanoi branch (~1140 km away) is filtered out by 10km radius; Pinned branch comes first
        assertThat(results).hasSize(2);
        assertThat(results.get(0).code()).isEqualTo("HCM-Q3-PINNED");
        assertThat(results.get(0).isPinned()).isTrue();
        assertThat(results.get(1).code()).isEqualTo("HCM-Q1-NEAR");
        assertThat(results.get(1).distanceMeters()).isLessThan(1000L);
    }

    @Test
    @DisplayName("SystemConfigService encrypts API secrets with AES-256-GCM at rest and masks them in Admin DTOs")
    void shouldEncryptSecretsAtRestAndMaskInAdminResponse() {
        when(systemConfigRepository.findById("VNPAY_HASH_SECRET")).thenReturn(Optional.empty());
        when(systemConfigRepository.save(any(SystemConfigEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpsertSystemConfigRequestDto req = new UpsertSystemConfigRequestDto(
                "VNPAY_HASH_SECRET",
                "PAYMENT",
                "VNPAY_LIVE_SECRET_9876",
                true
        );

        SystemConfigResponseDto adminDto = systemConfigService.upsertConfig(UUID.randomUUID(), req);

        // Admin DTO masks plaintext secret except last 4 chars
        assertThat(adminDto.isEncrypted()).isTrue();
        assertThat(adminDto.configValue()).isEqualTo("************9876");
    }

    @Test
    @DisplayName("SystemConfigService sanitizes B2B Policy HTML to prevent Stored XSS")
    void shouldSanitizePolicyHtmlAgainstStoredXss() {
        when(systemConfigRepository.findById(SystemConfigService.POLICY_CONFIG_KEY)).thenReturn(Optional.empty());
        when(systemConfigRepository.save(any(SystemConfigEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String maliciousHtml = "<h3>Wholesale Policy</h3><script>alert('XSS')</script><p onclick=\"steal()\">Free samples available</p>";
        PolicyResponseDto response = systemConfigService.updateB2bPolicy(
                UUID.randomUUID(),
                new UpdatePolicyRequestDto(maliciousHtml)
        );

        assertThat(response.policyHtml())
                .contains("<h3>Wholesale Policy</h3>")
                .contains("Free samples available")
                .doesNotContain("<script>")
                .doesNotContain("onclick");
    }
}
