package com.b2b.warehouse.order.dto;

import com.b2b.warehouse.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AdminUpdateOrderStatusRequestDto(
        @NotNull(message = "Target order status is required")
        OrderStatus targetStatus,

        @Size(max = 80)
        String trackingNumber,

        @Size(max = 1000)
        String adminMemo
) {
}
