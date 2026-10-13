package com.b2b.warehouse.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignAdminRequestDto(
        @NotNull(message = "assignedAdminId is required")
        UUID assignedAdminId
) {
}
