package com.b2b.warehouse.systemconfig.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertSystemConfigRequestDto(
        @NotBlank(message = "configKey is required")
        @Size(max = 100)
        String configKey,

        @NotBlank(message = "configGroup is required (PAYMENT, SHIPPING, MAP, POLICY)")
        @Size(max = 40)
        String configGroup,

        @NotBlank(message = "configValue is required")
        String configValue,

        Boolean isEncrypted
) {
}
