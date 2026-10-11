package com.b2b.warehouse.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Standardized domain and infrastructure error codes mapped to HTTP statuses.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Generic & Validation
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "ERR_VALIDATION", "Request validation failed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_NOT_FOUND", "Requested resource was not found"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "ERR_CONCURRENT_MODIFICATION", "Resource was modified by another transaction. Please retry."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_INTERNAL", "An unexpected internal error occurred"),

    // Auth & RBAC
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ERR_UNAUTHORIZED", "Authentication is required to access this resource"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ERR_INVALID_CREDENTIALS", "Invalid email or password"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "ERR_TOKEN_EXPIRED", "Authentication token has expired"),
    TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "ERR_TOKEN_REUSE_DETECTED", "Suspicious token reuse detected. All active sessions have been revoked."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "ERR_FORBIDDEN", "You do not have permission to perform this action"),
    PARTNER_NOT_ACTIVE(HttpStatus.FORBIDDEN, "ERR_PARTNER_NOT_ACTIVE", "Your B2B Partner account is not yet active. Please complete the 3-stage onboarding workflow."),

    // B2B Partner Onboarding & Files
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "ERR_DUPLICATE_EMAIL", "An account with this email already exists"),
    DUPLICATE_TAX_CODE(HttpStatus.CONFLICT, "ERR_DUPLICATE_TAX_CODE", "A B2B partner with this business tax code is already registered"),
    INVALID_ONBOARDING_STATE(HttpStatus.BAD_REQUEST, "ERR_INVALID_ONBOARDING_STATE", "Action is not allowed in the current onboarding status"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "ERR_INVALID_FILE_TYPE", "Uploaded file magic bytes do not match allowed types (PDF, JPEG, PNG, WEBP)"),

    // Inventory & Orders
    INSUFFICIENT_INVENTORY(HttpStatus.CONFLICT, "ERR_INSUFFICIENT_INVENTORY", "Insufficient available stock at the selected warehouse branch"),
    SAMPLE_QUOTA_EXCEEDED(HttpStatus.BAD_REQUEST, "ERR_SAMPLE_QUOTA_EXCEEDED", "Requested sample quantity exceeds the allowed per-product or monthly limit"),
    PAYMENT_SIGNATURE_INVALID(HttpStatus.BAD_REQUEST, "ERR_PAYMENT_SIGNATURE", "Invalid payment gateway webhook signature"),

    // Cryptography
    CRYPTO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_CRYPTO", "Failed to encrypt or decrypt sensitive configuration value");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;
}
