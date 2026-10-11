package com.b2b.warehouse.order.dto;

import com.b2b.warehouse.order.entity.OrderStatus;
import com.b2b.warehouse.order.entity.OrderType;
import com.b2b.warehouse.order.entity.PaymentMethod;
import com.b2b.warehouse.order.entity.PaymentStatus;
import com.b2b.warehouse.order.entity.ShippingProvider;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderResponseDto(
        UUID id,
        String orderCode,
        UUID userId,
        String userEmail,
        UUID branchId,
        String branchCode,
        String branchName,
        OrderType orderType,
        OrderStatus orderStatus,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        ShippingProvider shippingProvider,
        String trackingNumber,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String recipientName,
        String recipientPhone,
        String shippingPostalCode,
        String shippingAddressLine,
        String shippingAddressDetail,
        String orderNote,
        String adminMemo,
        List<OrderItemResponseDto> items,
        Instant paidAt,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        Instant createdAt
) {
}
