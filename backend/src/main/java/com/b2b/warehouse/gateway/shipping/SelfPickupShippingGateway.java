package com.b2b.warehouse.gateway.shipping;

import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.order.entity.ShippingProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SelfPickupShippingGateway implements ShippingGatewayStrategy {

    @Override
    public ShippingProvider getProvider() {
        return ShippingProvider.SELF_PICKUP;
    }

    @Override
    public ShippingQuoteDto calculateQuote(
            BranchEntity originBranch,
            int totalWeightGrams,
            Double destLatitude,
            Double destLongitude
    ) {
        return ShippingQuoteDto.builder()
                .provider(ShippingProvider.SELF_PICKUP)
                .shippingFee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .estimatedDeliveryTime("Self-Pickup at Warehouse Branch")
                .distanceKm(0.0)
                .totalWeightGrams(Math.max(totalWeightGrams, 0))
                .build();
    }
}
