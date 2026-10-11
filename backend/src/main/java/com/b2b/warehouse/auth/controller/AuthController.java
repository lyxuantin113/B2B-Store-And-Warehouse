package com.b2b.warehouse.auth.controller;

import com.b2b.warehouse.auth.dto.AuthTokenResponseDto;
import com.b2b.warehouse.auth.dto.B2bRegisterRequestDto;
import com.b2b.warehouse.auth.dto.ChangePasswordRequestDto;
import com.b2b.warehouse.auth.dto.LoginRequestDto;
import com.b2b.warehouse.auth.dto.RefreshTokenRequestDto;
import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.auth.service.AuthService;
import com.b2b.warehouse.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "01. Authentication & B2B Registration", description = "JWT Dual-Token Auth, Refresh Rotation & Stage 1 B2B Registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-b2b")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new B2B Partner account (Stage 1: PENDING_ADMIN) and issue tracking tokens")
    public ApiResponse<AuthTokenResponseDto> registerB2b(@Valid @RequestBody B2bRegisterRequestDto request) {
        AuthTokenResponseDto response = authService.registerB2bPartner(request);
        return ApiResponse.ok(response, "B2B registration submitted. Your account is now in Stage 1 (Pending Admin Review).");
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT Access + Refresh tokens")
    public ApiResponse<AuthTokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ApiResponse.ok(authService.login(request), "Login successful");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate single-use Refresh Token and issue a new Dual-Token pair")
    public ApiResponse<AuthTokenResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        return ApiResponse.ok(authService.refreshToken(request), "Token rotated successfully");
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke Refresh Token family in Redis")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequestDto request) {
        if (request != null) {
            authService.logout(request.refreshToken());
        }
        return ApiResponse.ok(null, "Logged out successfully");
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password and revoke all active sessions across devices")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequestDto request
    ) {
        authService.changePassword(currentUser.getUserId(), request);
        return ApiResponse.ok(null, "Password changed successfully. All previous sessions have been revoked.");
    }
}
