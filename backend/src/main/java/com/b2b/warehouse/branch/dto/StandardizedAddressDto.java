package com.b2b.warehouse.branch.dto;

import lombok.Builder;

@Builder
public record StandardizedAddressDto(
        String rawQuery,
        String formattedAddress,
        double latitude,
        double longitude,
        String postalCode,
        String placeId,
        String provider
) {
}
