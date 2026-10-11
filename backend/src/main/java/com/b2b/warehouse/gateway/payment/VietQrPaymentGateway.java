package com.b2b.warehouse.gateway.payment;

import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * NAPAS247 VietQR / PayOS Dynamic QR Bank Transfer Adapter for high-value B2B wholesale invoices.
 */
@Component
public class VietQrPaymentGateway implements PaymentGatewayStrategy {

    @Value("${vietqr.checksum-key:DEMO_VIETQR_CHECKSUM_KEY}")
    private String checksumKey;

    @Override
    public List<PaymentMethod> getSupportedMethods() {
        return List.of(PaymentMethod.VIETQR, PaymentMethod.BANK_TRANSFER);
    }

    @Override
    public PaymentInitiationDto initiatePayment(OrderEntity order) {
        String idempotencyKey = "VIETQR-" + order.getOrderCode() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String transferMemo = "B2B " + order.getOrderCode();
        String qrPayload = "00020101021238570010A0000007270127000697043601131900888899999"
                + "530370454" + order.getTotalAmount().longValue()
                + "5802VN62" + transferMemo;

        return PaymentInitiationDto.builder()
                .paymentMethod(order.getPaymentMethod())
                .paymentUrl("https://img.vietqr.io/image/VCB-1900888899999-compact2.png?amount="
                        + order.getTotalAmount().longValue() + "&addInfo=" + order.getOrderCode())
                .qrCodeData(qrPayload)
                .transferMemo(transferMemo)
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
        String expectedSignature = HmacSignUtil.hmacSha256Hex(checksumKey, canonicalData);
        return HmacSignUtil.constantTimeEquals(expectedSignature, payload.signature());
    }
}
