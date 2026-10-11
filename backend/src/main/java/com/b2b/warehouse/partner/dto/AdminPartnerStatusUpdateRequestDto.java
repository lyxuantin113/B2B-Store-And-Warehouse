package com.b2b.warehouse.partner.dto;

import com.b2b.warehouse.auth.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AdminPartnerStatusUpdateRequestDto(
        @NotNull(message = "Target account status is required")
        AccountStatus targetStatus,

        @Size(max = 2000)
        String approvalNotice,

        @Size(max = 2000)
        String rejectionReason
) {
}
