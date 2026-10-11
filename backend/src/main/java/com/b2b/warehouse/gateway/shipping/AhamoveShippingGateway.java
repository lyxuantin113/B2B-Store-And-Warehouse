package com.b2b.warehouse.gateway.shipping;

import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.order.entity.ShippingProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * AhaMove / GrabExpress Same-Day On-Demand Courier Adapter (replaces Korean Kakao T Quick).
 * Computes real GPS distance between the origin {@link BranchEntity} and destination coordinates.
 * Base fee: 25,000 VND for first 2 km + 5,500 VND per additional km.
 */
@Component
public class AhamoveShippingGateway implements ShippingGatewayStrategy {

    private static final BigDecimal BASE_2KM_FEE_VND = new BigDecimal("25000.00");
    private static final BigDecimal PER_EXTRA_KM_FEE_VND = new BigDecimal("5500.00");
    private static final double DEFAULT_FALLBACK_DISTANCE_KM = 6.0;

    @Override
    public ShippingProvider getProvider() {
        return ShippingProvider.AHAMOVE;
    }

    @Override
    public ShippingQuoteDto calculateQuote(
            BranchEntity originBranch,
            int totalWeightGrams,
            Double destLatitude,
            Double destLongitude
    ) {
        double distanceKm = DEFAULT_FALLBACK_DISTANCE_KM;
        if (originBranch != null
                && originBranch.getLatitude() != null
                && originBranch.getLongitude() != null
                && destLatitude != null
                && destLongitude != null) {
            distanceKm = calculateHaversineKm(
                    originBranch.getLatitude(),
                    originBranch.getLongitude(),
                    destLatitude,
                    destLongitude
            );
        }

        double roundedDistance = Math.max(1.0, Math.round(distanceKm * 10.0) / 10.0);
        BigDecimal fee = BASE_2KM_FEE_VND;
        if (roundedDistance > 2.0) {
            double extraKm = Math.ceil(roundedDistance - 2.0);
            fee = fee.add(PER_EXTRA_KM_FEE_VND.multiply(BigDecimal.valueOf(extraKm)));
        }

        return ShippingQuoteDto.builder()
                .provider(ShippingProvider.AHAMOVE)
                .shippingFee(fee.setScale(2, RoundingMode.HALF_UP))
                .estimatedDeliveryTime("AhaMove Instant Dispatch (2~4 hours)")
                .distanceKm(roundedDistance)
                .totalWeightGrams(Math.max(totalWeightGrams, 100))
                .build();
    }

    public static double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
