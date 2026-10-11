package com.b2b.warehouse.partner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Request DTO for B2B Partner self-updating their profile in B2B Portal Tab 8.
 * Legal fields ({@code companyName}, {@code taxCode}, {@code representativeName}) are intentionally omitted
 * so partners cannot tamper with verified legal registration data after approval.
 */
@Builder
public record PartnerSelfUpdateProfileRequestDto(
        @NotBlank(message = "Contact person name is required")
        @Size(max = 120)
        String contactPersonName,

        @NotBlank(message = "Contact phone is required")
        @Pattern(regexp = "^[0-9+\\-()\\s]{8,20}$", message = "Invalid phone number format")
        String contactPhone,

        @Size(max = 20)
        String shippingPostalCode,

        @NotBlank(message = "Default shipping address is required")
        @Size(max = 255)
        String shippingAddressLine,

        @Size(max = 255)
        String shippingAddressDetail
) {
}
