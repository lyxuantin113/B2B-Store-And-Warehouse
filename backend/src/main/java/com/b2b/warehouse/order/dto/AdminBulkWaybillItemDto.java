package com.b2b.warehouse.order.dto;

import com.b2b.warehouse.order.entity.ShippingProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AdminBulkWaybillItemDto(
        @NotBlank(message = "Order code is required")
        String orderCode,

        @NotBlank(message = "Tracking number is required")
        @Size(max = 80)
        String trackingNumber,

        ShippingProvider shippingProvider
) {
}
