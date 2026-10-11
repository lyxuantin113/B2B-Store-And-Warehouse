package com.b2b.warehouse.order.dto;

import com.b2b.warehouse.gateway.payment.PaymentInitiationDto;
import com.b2b.warehouse.gateway.shipping.ShippingQuoteDto;
import lombok.Builder;

@Builder
public record OrderCheckoutResponseDto(
        OrderResponseDto order,
        PaymentInitiationDto payment,
        ShippingQuoteDto shippingQuote
) {
}
