package com.b2b.warehouse.gateway.payment;

import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayRouter {

    private final Map<PaymentMethod, PaymentGatewayStrategy> strategies = new EnumMap<>(PaymentMethod.class);

    public PaymentGatewayRouter(List<PaymentGatewayStrategy> strategyBeans) {
        for (PaymentGatewayStrategy strategy : strategyBeans) {
            for (PaymentMethod method : strategy.getSupportedMethods()) {
                strategies.put(method, strategy);
            }
        }
    }

    public PaymentInitiationDto initiatePayment(OrderEntity order) {
        return getStrategy(order.getPaymentMethod()).initiatePayment(order);
    }

    public boolean verifyWebhook(PaymentMethod method, PaymentWebhookPayloadDto payload) {
        return getStrategy(method).verifyWebhookSignature(payload);
    }

    private PaymentGatewayStrategy getStrategy(PaymentMethod method) {
        PaymentGatewayStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported payment method: " + method);
        }
        return strategy;
    }
}
