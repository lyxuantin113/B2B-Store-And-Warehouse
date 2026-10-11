package com.b2b.warehouse.gateway.payment;

import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.PaymentMethod;

import java.util.List;

/**
 * Strategy interface for Vietnam payment gateways (replacing Korean Toss, KakaoPay, NaverPay).
 */
public interface PaymentGatewayStrategy {

    List<PaymentMethod> getSupportedMethods();

    PaymentInitiationDto initiatePayment(OrderEntity order);

    boolean verifyWebhookSignature(PaymentWebhookPayloadDto payload);
}
