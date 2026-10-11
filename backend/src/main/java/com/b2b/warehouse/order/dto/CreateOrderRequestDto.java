package com.b2b.warehouse.order.dto;

import com.b2b.warehouse.order.entity.PaymentMethod;
import com.b2b.warehouse.order.entity.ShippingProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CreateOrderRequestDto(
        @NotNull(message = "Origin warehouse branch ID is required")
        UUID branchId,

        @NotNull(message = "Payment method is required (VNPAY, MOMO, VIETQR, BANK_TRANSFER)")
        PaymentMethod paymentMethod,

        @NotNull(message = "Shipping provider is required (GHN, GHTK, AHAMOVE, SELF_PICKUP)")
        ShippingProvider shippingProvider,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 120)
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        @Pattern(regexp = "^[0-9+\\-()\\s]{8,20}$")
        String recipientPhone,

        @Size(max = 20)
        String shippingPostalCode,

        @NotBlank(message = "Shipping address line is required")
        @Size(max = 255)
        String shippingAddressLine,

        @Size(max = 255)
        String shippingAddressDetail,

        Double destLatitude,
        Double destLongitude,

        @Size(max = 1000)
        String orderNote,

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderLineItemRequestDto> items
) {
}
