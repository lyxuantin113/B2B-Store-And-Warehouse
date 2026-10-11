package com.b2b.warehouse.partner.dto;

import com.b2b.warehouse.auth.entity.AccountStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AdminBulkPartnerStatusRequestDto(
        @NotEmpty(message = "At least one partner ID must be selected")
        List<UUID> partnerIds,

        @NotNull(message = "Target account status is required")
        AccountStatus targetStatus,

        @Size(max = 2000)
        String noticeOrReason
) {
}
