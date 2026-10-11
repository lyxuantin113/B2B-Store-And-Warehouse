package com.b2b.warehouse.gateway.shipping;

import com.b2b.warehouse.order.entity.ShippingProvider;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ShippingQuoteDto(
        ShippingProvider provider,
        BigDecimal shippingFee,
        String estimatedDeliveryTime,
        Double distanceKm,
        int totalWeightGrams
) {
}
