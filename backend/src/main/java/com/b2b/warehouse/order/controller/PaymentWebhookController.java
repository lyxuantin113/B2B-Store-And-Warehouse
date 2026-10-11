package com.b2b.warehouse.order.controller;

import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.gateway.payment.PaymentWebhookPayloadDto;
import com.b2b.warehouse.order.dto.OrderResponseDto;
import com.b2b.warehouse.order.entity.PaymentMethod;
import com.b2b.warehouse.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/payments")
@RequiredArgsConstructor
@Tag(name = "11. Payment Gateway Webhooks", description = "Idempotent HMAC-Verified Webhooks for VNPay, MoMo, and VietQR")
public class PaymentWebhookController {

    private final OrderService orderService;

    @PostMapping("/{provider}")
    @Operation(summary = "Process idempotent payment IPN/webhook from VNPAY, MOMO, or VIETQR")
    public ApiResponse<OrderResponseDto> handleWebhook(
            @PathVariable PaymentMethod provider,
            @Valid @RequestBody PaymentWebhookPayloadDto payload
    ) {
        OrderResponseDto updated = orderService.handlePaymentWebhook(provider, payload);
        return ApiResponse.ok(updated, "Webhook processed successfully");
    }
}
