package com.b2b.warehouse.gateway.payment;

import com.b2b.warehouse.order.entity.PaymentMethod;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentInitiationDto(
        PaymentMethod paymentMethod,
        String paymentUrl,
        String qrCodeData,
        String transferMemo,
        String idempotencyKey,
        BigDecimal amount
) {
}
