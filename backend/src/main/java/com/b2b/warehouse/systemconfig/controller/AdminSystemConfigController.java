package com.b2b.warehouse.systemconfig.controller;

import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.systemconfig.dto.PolicyResponseDto;
import com.b2b.warehouse.systemconfig.dto.SystemConfigResponseDto;
import com.b2b.warehouse.systemconfig.dto.UpdatePolicyRequestDto;
import com.b2b.warehouse.systemconfig.dto.UpsertSystemConfigRequestDto;
import com.b2b.warehouse.systemconfig.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system-configs")
@RequiredArgsConstructor
@Tag(name = "Admin System Configurations & Policy CMS", description = "Manage encrypted VNPay/MoMo/VietQR/GHN/GHTK/AhaMove/GoogleMaps API keys and B2B Policy HTML")
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @PreAuthorize("hasAuthority('config:manage')")
    @Operation(summary = "List dynamic system configs (encrypted secrets are automatically masked)")
    public ResponseEntity<ApiResponse<List<SystemConfigResponseDto>>> listConfigs(
            @RequestParam(required = false) String configGroup
    ) {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigService.listConfigsForAdmin(configGroup)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('config:manage')")
    @Operation(summary = "Create or update a system configuration (encrypts with AES-256-GCM when isEncrypted=true)")
    public ResponseEntity<ApiResponse<SystemConfigResponseDto>> upsertConfig(
            @AuthenticationPrincipal AuthenticatedUser adminUser,
            @Valid @RequestBody UpsertSystemConfigRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                systemConfigService.upsertConfig(adminUser.getUserId(), request),
                "System configuration saved"
        ));
    }

    @PutMapping("/policy")
    @PreAuthorize("hasAuthority('policy:write')")
    @Operation(summary = "Update the B2B Wholesale & Sample Policy CMS page content (sanitized via OWASP HTML Sanitizer)")
    public ResponseEntity<ApiResponse<PolicyResponseDto>> updatePolicy(
            @AuthenticationPrincipal AuthenticatedUser adminUser,
            @Valid @RequestBody UpdatePolicyRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                systemConfigService.updateB2bPolicy(adminUser.getUserId(), request),
                "B2B Policy updated successfully"
        ));
    }
}
