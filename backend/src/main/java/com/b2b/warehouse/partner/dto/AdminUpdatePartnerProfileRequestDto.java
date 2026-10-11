package com.b2b.warehouse.partner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Allows Admin/Manager to update both legal and operational fields of a B2B partner.
 */
@Builder
public record AdminUpdatePartnerProfileRequestDto(
        @NotBlank(message = "Company name is required")
        @Size(max = 200)
        String companyName,

        @NotBlank(message = "Business tax code is required")
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

        @NotBlank(message = "Contact phone is required")
        @Pattern(regexp = "^[0-9+\\-()\\s]{8,20}$", message = "Invalid phone number format")
        String contactPhone,

        @NotBlank(message = "Business license URL is required")
        @Size(max = 500)
        String businessLicenseUrl,

        @Size(max = 20)
        String postalCode,

        @NotBlank(message = "Address line is required")
        @Size(max = 255)
        String addressLine,

        @Size(max = 255)
        String addressDetail,

        @Size(max = 20)
        String shippingPostalCode,

        @Size(max = 255)
        String shippingAddressLine,

        @Size(max = 255)
        String shippingAddressDetail
) {
}
