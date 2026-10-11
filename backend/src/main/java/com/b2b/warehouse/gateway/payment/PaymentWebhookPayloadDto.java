package com.b2b.warehouse.gateway.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentWebhookPayloadDto(
        @NotBlank(message = "Order code is required")
        String orderCode,

        @NotBlank(message = "External transaction ID is required")
        String externalTxnId,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00")
        BigDecimal amount,

        @NotBlank(message = "Status is required (SUCCESS or FAILED)")
        String status,

        @NotBlank(message = "Cryptographic signature is required")
        String signature
) {
}
