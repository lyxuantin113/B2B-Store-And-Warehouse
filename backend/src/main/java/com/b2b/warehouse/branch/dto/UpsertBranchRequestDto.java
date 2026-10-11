package com.b2b.warehouse.branch.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpsertBranchRequestDto(
        @NotBlank(message = "Branch code is required")
        @Pattern(regexp = "^[A-Za-z0-9_\\-]{2,30}$", message = "Branch code must be 2-30 alphanumeric/hyphen characters")
        String code,

        @NotBlank(message = "Branch name is required")
        @Size(max = 150)
        String name,

        @Size(max = 80)
        String categoryTag,

        @Size(max = 25)
        String phone,

        @Size(max = 20)
        String postalCode,

        @NotBlank(message = "Address line is required")
        @Size(max = 255)
        String addressLine,

        @Size(max = 255)
        String addressDetail,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double longitude,

        Boolean isPinned,
        Boolean isActive,

        @Size(max = 255)
        String operatingHours
) {
}
