package com.b2b.warehouse.gateway.shipping;

import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.order.entity.ShippingProvider;

/**
 * Strategy interface for Vietnam logistics &amp; courier providers
 * (replacing Korean Logen Express and Kakao T Quick).
 */
public interface ShippingGatewayStrategy {

    ShippingProvider getProvider();

    ShippingQuoteDto calculateQuote(
            BranchEntity originBranch,
            int totalWeightGrams,
            Double destLatitude,
            Double destLongitude
    );
}
