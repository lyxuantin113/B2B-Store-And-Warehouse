package com.b2b.warehouse.gateway.payment;

import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * MoMo e-Wallet B2B Payment Gateway Adapter using HMAC-SHA256 signature verification.
 */
@Component
public class MomoPaymentGateway implements PaymentGatewayStrategy {

    @Value("${momo.partner-code:DEMO_MOMO_PARTNER}")
    private String partnerCode;

    @Value("${momo.secret-key:DEMO_MOMO_SECRET_KEY}")
    private String secretKey;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String endpoint;

    @Override
    public List<PaymentMethod> getSupportedMethods() {
        return List.of(PaymentMethod.MOMO);
    }

    @Override
    public PaymentInitiationDto initiatePayment(OrderEntity order) {
        String idempotencyKey = "MOMO-" + order.getOrderCode() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String rawData = "partnerCode=" + partnerCode
                + "&orderId=" + order.getOrderCode()
                + "&amount=" + order.getTotalAmount().longValue()
                + "&requestId=" + idempotencyKey;
        String signature = HmacSignUtil.hmacSha256Hex(secretKey, rawData);

        String redirectUrl = endpoint + "?" + rawData + "&signature=" + signature;

        return PaymentInitiationDto.builder()
                .paymentMethod(PaymentMethod.MOMO)
                .paymentUrl(redirectUrl)
                .transferMemo("MOMO " + order.getOrderCode())
                .idempotencyKey(idempotencyKey)
                .amount(order.getTotalAmount())
                .build();
    }

    @Override
    public boolean verifyWebhookSignature(PaymentWebhookPayloadDto payload) {
        String canonicalData = payload.orderCode() + "|"
                + payload.externalTxnId() + "|"
                + payload.idempotencyKey() + "|"
                + payload.amount().setScale(2) + "|"
                + payload.status();
        String expectedSignature = HmacSignUtil.hmacSha256Hex(secretKey, canonicalData);
        return HmacSignUtil.constantTimeEquals(expectedSignature, payload.signature());
    }
}
