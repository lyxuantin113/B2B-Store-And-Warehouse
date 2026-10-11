package com.b2b.warehouse.systemconfig.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePolicyRequestDto(
        @NotBlank(message = "policyHtml is required")
        String policyHtml
) {
}
