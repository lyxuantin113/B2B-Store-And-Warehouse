package com.b2b.warehouse.gateway.shipping;

import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.order.entity.ShippingProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Giao Hang Tiet Kiem (GHTK) economy nationwide B2B parcel shipping adapter.
 * Base fee: 25,000 VND for first 2,000g + 4,000 VND per additional 500g block.
 */
@Component
public class GhtkShippingGateway implements ShippingGatewayStrategy {

    private static final BigDecimal BASE_FEE_VND = new BigDecimal("25000.00");
    private static final int BASE_WEIGHT_GRAMS = 2000;
    private static final BigDecimal EXTRA_500G_FEE_VND = new BigDecimal("4000.00");

    @Override
    public ShippingProvider getProvider() {
        return ShippingProvider.GHTK;
    }

    @Override
    public ShippingQuoteDto calculateQuote(
            BranchEntity originBranch,
            int totalWeightGrams,
            Double destLatitude,
            Double destLongitude
    ) {
        int safeWeight = Math.max(totalWeightGrams, 100);
        BigDecimal fee = BASE_FEE_VND;
        if (safeWeight > BASE_WEIGHT_GRAMS) {
            int extraGrams = safeWeight - BASE_WEIGHT_GRAMS;
            int blocks = (int) Math.ceil(extraGrams / 500.0);
            fee = fee.add(EXTRA_500G_FEE_VND.multiply(BigDecimal.valueOf(blocks)));
        }

        return ShippingQuoteDto.builder()
                .provider(ShippingProvider.GHTK)
                .shippingFee(fee.setScale(2, RoundingMode.HALF_UP))
                .estimatedDeliveryTime("GHTK Standard (2~3 business days)")
                .totalWeightGrams(safeWeight)
                .build();
    }
}
