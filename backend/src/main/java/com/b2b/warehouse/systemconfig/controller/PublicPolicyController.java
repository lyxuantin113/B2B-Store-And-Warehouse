package com.b2b.warehouse.systemconfig.controller;

import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.systemconfig.dto.PolicyResponseDto;
import com.b2b.warehouse.systemconfig.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Public & B2B Policy CMS", description = "Read-only B2B Wholesale & Free Sample Policy content managed by Admin")
public class PublicPolicyController {

    private final SystemConfigService systemConfigService;

    @GetMapping({"/api/v1/public/policy", "/api/v1/b2b/policy"})
    @Operation(summary = "Get B2B Wholesale & Sample Policy content")
    public ResponseEntity<ApiResponse<PolicyResponseDto>> getPolicy() {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigService.getB2bPolicy()));
    }
}
