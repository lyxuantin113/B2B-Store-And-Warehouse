package com.b2b.warehouse.gateway.payment;

import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * VNPay Payment Gateway Adapter using HMAC-SHA512 signature verification.
 */
@Component
public class VnPayPaymentGateway implements PaymentGatewayStrategy {

    @Value("${vnpay.tmn-code:DEMO_VNPAY_TMN}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:DEMO_VNPAY_HASH_SECRET_KEY_2026}")
    private String hashSecret;

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Override
    public List<PaymentMethod> getSupportedMethods() {
        return List.of(PaymentMethod.VNPAY);
    }

    @Override
    public PaymentInitiationDto initiatePayment(OrderEntity order) {
        String idempotencyKey = "VNPAY-" + order.getOrderCode() + "-" + UUID.randomUUID().toString().substring(0, 8);
        long vnpAmount = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        String canonicalQuery = "vnp_Amount=" + vnpAmount
                + "&vnp_Command=pay"
                + "&vnp_CurrCode=VND"
                + "&vnp_OrderInfo=" + URLEncoder.encode("B2B Order " + order.getOrderCode(), StandardCharsets.UTF_8)
                + "&vnp_TmnCode=" + tmnCode
                + "&vnp_TxnRef=" + order.getOrderCode();

        String secureHash = HmacSignUtil.hmacSha512Hex(hashSecret, canonicalQuery);
        String fullUrl = payUrl + "?" + canonicalQuery + "&vnp_SecureHash=" + secureHash;

        return PaymentInitiationDto.builder()
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentUrl(fullUrl)
                .transferMemo("VNPAY " + order.getOrderCode())
                .idempotencyKey(idempotencyKey)
                .amount(order.getTotalAmount())
                .build();
    }

    @Override
    public boolean verifyWebhookSignature(PaymentWebhookPayloadDto payload) {
        String canonicalData = buildCanonicalWebhookString(payload);
        String expectedSignature = HmacSignUtil.hmacSha512Hex(hashSecret, canonicalData);
        return HmacSignUtil.constantTimeEquals(expectedSignature, payload.signature());
    }

    public String computeWebhookSignatureForTest(PaymentWebhookPayloadDto payload) {
        return HmacSignUtil.hmacSha512Hex(hashSecret, buildCanonicalWebhookString(payload));
    }

    private String buildCanonicalWebhookString(PaymentWebhookPayloadDto payload) {
        return payload.orderCode() + "|"
                + payload.externalTxnId() + "|"
                + payload.idempotencyKey() + "|"
                + payload.amount().setScale(2) + "|"
                + payload.status();
    }
}
