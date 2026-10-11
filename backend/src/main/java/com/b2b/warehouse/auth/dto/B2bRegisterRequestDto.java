package com.b2b.warehouse.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Request DTO for Stage 1 B2B Partner Registration ({@code POST /api/v1/auth/register-b2b}).
 * Mirrors the Korean reference {@code b2b-register.php} fields adapted for Vietnam/Global B2B tax codes.
 */
@Builder
public record B2bRegisterRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank(message = "Company name is required")
        @Size(max = 200)
        String companyName,

        @NotBlank(message = "Business tax code (MST) is required")
        @Pattern(regexp = "^[0-9A-Za-z\\-]{8,20}$", message = "Tax code must be 8-20 alphanumeric/hyphen characters")
        String taxCode,

        @Size(max = 80)
        String businessType,

        @NotBlank(message = "Legal representative name is required")
        @Size(max = 120)
        String representativeName,

        @NotBlank(message = "Contact person name is required")
        @Size(max = 120)
        String contactPersonName,

        @NotBlank(message = "Contact phone number is required")
        @Pattern(regexp = "^[0-9+\\-()\\s]{8,20}$", message = "Invalid phone number format")
        String contactPhone,

        @NotBlank(message = "Business license document URL is required")
        @Size(max = 500)
        String businessLicenseUrl,

        @Size(max = 20)
        String postalCode,

        @NotBlank(message = "Business address is required")
        @Size(max = 255)
        String addressLine,

        @Size(max = 255)
        String addressDetail
) {
}
