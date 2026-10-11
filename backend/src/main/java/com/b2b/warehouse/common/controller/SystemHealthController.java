package com.b2b.warehouse.common.controller;

import com.b2b.warehouse.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/health")
@Tag(name = "00. System Health", description = "Public liveness and readiness verification endpoint")
public class SystemHealthController {

    @GetMapping
    @Operation(summary = "Check API liveness and version metadata")
    public ApiResponse<Map<String, Object>> checkHealth() {
        Map<String, Object> status = Map.of(
                "service", "b2b-warehouse-backend",
                "status", "UP",
                "module", "Module 1 - Infrastructure & Backend Core",
                "serverTime", Instant.now().toString()
        );
        return ApiResponse.ok(status, "B2B Wholesale & Sample Platform API is healthy");
    }
}
