package com.b2b.warehouse.gateway.shipping;

import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.order.entity.ShippingProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ShippingGatewayRouter {

    private final Map<ShippingProvider, ShippingGatewayStrategy> strategies = new EnumMap<>(ShippingProvider.class);

    public ShippingGatewayRouter(List<ShippingGatewayStrategy> strategyBeans) {
        for (ShippingGatewayStrategy strategy : strategyBeans) {
            strategies.put(strategy.getProvider(), strategy);
        }
    }

    public ShippingQuoteDto calculateShippingQuote(
            ShippingProvider provider,
            BranchEntity originBranch,
            int totalWeightGrams,
            Double destLatitude,
            Double destLongitude
    ) {
        ShippingGatewayStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported shipping provider: " + provider);
        }
        return strategy.calculateQuote(originBranch, totalWeightGrams, destLatitude, destLongitude);
    }
}
