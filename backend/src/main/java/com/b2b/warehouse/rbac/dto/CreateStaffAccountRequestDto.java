package com.b2b.warehouse.rbac.dto;

import com.b2b.warehouse.rbac.entity.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateStaffAccountRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72)
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 120)
        String fullName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9+\\-()\\s]{8,20}$")
        String phone,

        @NotNull(message = "Role code is required (ADMIN, MANAGER, or SELLER)")
        RoleCode roleCode
) {
}
