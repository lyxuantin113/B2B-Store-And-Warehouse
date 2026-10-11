package com.b2b.warehouse.order.service;

import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.auth.repository.UserRepository;
import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.branch.repository.BranchRepository;
import com.b2b.warehouse.catalog.dto.WholesalePriceQuoteDto;
import com.b2b.warehouse.catalog.entity.ProductEntity;
import com.b2b.warehouse.catalog.repository.ProductRepository;
import com.b2b.warehouse.catalog.service.DynamicPriceTierValidator;
import com.b2b.warehouse.common.dto.CursorPageDto;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.gateway.payment.PaymentGatewayRouter;
import com.b2b.warehouse.gateway.payment.PaymentInitiationDto;
import com.b2b.warehouse.gateway.payment.PaymentWebhookPayloadDto;
import com.b2b.warehouse.gateway.shipping.ShippingGatewayRouter;
import com.b2b.warehouse.gateway.shipping.ShippingQuoteDto;
import com.b2b.warehouse.inventory.dto.StockReservationItemDto;
import com.b2b.warehouse.inventory.service.InventoryService;
import com.b2b.warehouse.order.dto.AdminBulkWaybillItemDto;
import com.b2b.warehouse.order.dto.AdminSalesAnalyticsKpiDto;
import com.b2b.warehouse.order.dto.AdminUpdateOrderStatusRequestDto;
import com.b2b.warehouse.order.dto.CreateOrderRequestDto;
import com.b2b.warehouse.order.dto.OrderCheckoutResponseDto;
import com.b2b.warehouse.order.dto.OrderItemResponseDto;
import com.b2b.warehouse.order.dto.OrderLineItemRequestDto;
import com.b2b.warehouse.order.dto.OrderResponseDto;
import com.b2b.warehouse.order.dto.PartnerDashboardOverviewDto;
import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.OrderItemEntity;
import com.b2b.warehouse.order.entity.OrderStatus;
import com.b2b.warehouse.order.entity.OrderType;
import com.b2b.warehouse.order.entity.PaymentMethod;
import com.b2b.warehouse.order.entity.PaymentStatus;
import com.b2b.warehouse.order.entity.PaymentTransactionEntity;
import com.b2b.warehouse.order.repository.OrderRepository;
import com.b2b.warehouse.order.repository.PaymentTransactionRepository;
import com.b2b.warehouse.outbox.entity.OutboxEventEntity;
import com.b2b.warehouse.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core B2B Wholesale &amp; Free Sample Order Engine.
 *
 * <p>Guarantees:</p>
 * <ul>
 *   <li><b>Server-Side Dynamic Tier Pricing</b>: Never trusts client-submitted prices; computes unit prices
 *       directly from the Admin-configured {@code product_price_tiers} brackets.</li>
 *   <li><b>Free Sample Pricing Rule</b>: For {@code SAMPLE} orders, enforces {@code sampleEnabled == true}
 *       and {@code quantity <= sampleMaxQty}, sets product unit price to {@code 0.00 VND}, and charges only shipping fee.</li>
 *   <li><b>Deadlock-Free Atomic Stock Reservation</b>: Reserves inventory via {@link InventoryService#reserveStockForOrder}.</li>
 *   <li><b>Transactional Outbox</b>: Persists {@link OutboxEventEntity} in the same DB transaction for Kafka delivery.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final DynamicPriceTierValidator priceTierValidator;
    private final InventoryService inventoryService;
    private final ShippingGatewayRouter shippingGatewayRouter;
    private final PaymentGatewayRouter paymentGatewayRouter;

    /**
     * Tab 2 One-Page Checkout: Places a B2B Wholesale Order using Admin-configured dynamic price tiers.
     */
    @Transactional
    public OrderCheckoutResponseDto placeWholesaleOrder(UUID userId, CreateOrderRequestDto request) {
        return createOrderInternal(userId, OrderType.WHOLESALE, request);
    }

    /**
     * Tab 7 Free Sample Request Flow: Places a B2B Sample Order where product unit price = 0 VND
     * and the partner only pays the carrier shipping fee (GHN, GHTK, or AhaMove).
     */
    @Transactional
    public OrderCheckoutResponseDto placeSampleOrder(UUID userId, CreateOrderRequestDto request) {
        return createOrderInternal(userId, OrderType.SAMPLE, request);
    }

    private OrderCheckoutResponseDto createOrderInternal(
            UUID userId,
            OrderType orderType,
            CreateOrderRequestDto request
    ) {
        UserEntity user = userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        BranchEntity branch = branchRepository.findById(request.branchId())
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Active warehouse branch not found"));

        // Merge duplicate productIds in the request cart
        Map<UUID, Integer> requestedQuantities = request.items().stream()
                .collect(Collectors.toMap(
                        OrderLineItemRequestDto::productId,
                        OrderLineItemRequestDto::quantity,
                        Integer::sum
                ));

        // Batch-fetch all products + dynamic price tiers in a single SQL query (Zero N+1)
        List<ProductEntity> products = productRepository.findAllWithDetailsByIdInAndDeletedAtIsNull(requestedQuantities.keySet());
        if (products.size() != requestedQuantities.size()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "One or more requested products do not exist or are inactive");
        }
        Map<UUID, ProductEntity> productMap = products.stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        // Reserve stock atomically across items (sorted by productId ASC inside InventoryService to prevent deadlocks)
        List<StockReservationItemDto> reservationItems = requestedQuantities.entrySet().stream()
                .map(e -> StockReservationItemDto.builder().productId(e.getKey()).quantity(e.getValue()).build())
                .toList();
        inventoryService.reserveStockForOrder(branch.getId(), reservationItems);

        OrderEntity order = OrderEntity.builder()
                .orderCode(generateOrderCode(orderType))
                .user(user)
                .branch(branch)
                .orderType(orderType)
                .orderStatus(OrderStatus.PENDING)
                .paymentMethod(request.paymentMethod())
                .paymentStatus(PaymentStatus.UNPAID)
                .shippingProvider(request.shippingProvider())
                .recipientName(request.recipientName().trim())
                .recipientPhone(request.recipientPhone().trim())
                .shippingPostalCode(request.shippingPostalCode())
                .shippingAddressLine(request.shippingAddressLine().trim())
                .shippingAddressDetail(request.shippingAddressDetail())
                .orderNote(request.orderNote())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int totalWeightGrams = 0;

        for (Map.Entry<UUID, Integer> entry : requestedQuantities.entrySet()) {
            ProductEntity product = productMap.get(entry.getKey());
            int qty = entry.getValue();
            totalWeightGrams += (product.getWeightGrams() == null ? 500 : product.getWeightGrams()) * qty;

            if (orderType == OrderType.SAMPLE) {
                if (!Boolean.TRUE.equals(product.getSampleEnabled())) {
                    throw new BusinessException(
                            ErrorCode.SAMPLE_QUOTA_EXCEEDED,
                            "Product " + product.getSku() + " is not eligible for free sample requests"
                    );
                }
                int maxSample = product.getSampleMaxQty() == null ? 2 : product.getSampleMaxQty();
                if (qty > maxSample) {
                    throw new BusinessException(
                            ErrorCode.SAMPLE_QUOTA_EXCEEDED,
                            "Sample quantity (" + qty + ") for " + product.getSku() + " exceeds limit of " + maxSample
                    );
                }

                order.addItem(OrderItemEntity.builder()
                        .product(product)
                        .skuSnapshot(product.getSku())
                        .productNameSnap(product.getName())
                        .unitLabelSnap(product.getUnitLabel())
                        .appliedTierCode("FREE_SAMPLE")
                        .quantity(qty)
                        .unitPrice(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .lineTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .build());
            } else {
                WholesalePriceQuoteDto quote = priceTierValidator.calculateQuote(product, qty);
                subtotal = subtotal.add(quote.lineTotal());
                totalDiscount = totalDiscount.add(quote.totalSavingsVsRetail());

                order.addItem(OrderItemEntity.builder()
                        .product(product)
                        .skuSnapshot(product.getSku())
                        .productNameSnap(product.getName())
                        .unitLabelSnap(product.getUnitLabel())
                        .appliedTierCode(quote.appliedTierCode())
                        .quantity(qty)
                        .unitPrice(quote.appliedUnitPrice())
                        .lineTotal(quote.lineTotal())
                        .build());
            }
        }

        ShippingQuoteDto shippingQuote = shippingGatewayRouter.calculateShippingQuote(
                request.shippingProvider(),
                branch,
                totalWeightGrams,
                request.destLatitude(),
                request.destLongitude()
        );

        BigDecimal shippingFee = shippingQuote.shippingFee();
        BigDecimal totalAmount = subtotal.add(shippingFee).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotalAmount(subtotal);
        order.setDiscountAmount(totalDiscount);
        order.setShippingFee(shippingFee);
        order.setTaxAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setTotalAmount(totalAmount);

        OrderEntity savedOrder = orderRepository.save(order);

        PaymentInitiationDto paymentInit = paymentGatewayRouter.initiatePayment(savedOrder);

        PaymentTransactionEntity initialTxn = PaymentTransactionEntity.builder()
                .order(savedOrder)
                .provider(savedOrder.getPaymentMethod().name())
                .idempotencyKey(paymentInit.idempotencyKey())
                .amount(savedOrder.getTotalAmount())
                .status("INITIATED")
                .build();
        paymentTransactionRepository.save(initialTxn);

        saveOutboxEvent(
                "ORDER",
                savedOrder.getOrderCode(),
                "ORDER_CREATED",
                Map.of(
                        "orderId", savedOrder.getId() != null ? savedOrder.getId().toString() : "",
                        "orderCode", savedOrder.getOrderCode(),
                        "userId", user.getId().toString(),
                        "orderType", savedOrder.getOrderType().name(),
                        "totalAmount", savedOrder.getTotalAmount()
                )
        );

        return OrderCheckoutResponseDto.builder()
                .order(toOrderDto(savedOrder))
                .payment(paymentInit)
                .shippingQuote(shippingQuote)
                .build();
    }

    /**
     * Idempotent Payment Webhook Handler for VNPay, MoMo, and VietQR callbacks.
     */
    @Transactional
    public OrderResponseDto handlePaymentWebhook(PaymentMethod provider, PaymentWebhookPayloadDto payload) {
        if (!paymentGatewayRouter.verifyWebhook(provider, payload)) {
            throw new BusinessException(ErrorCode.PAYMENT_SIGNATURE_INVALID);
        }

        OrderEntity order = orderRepository.findWithDetailsByOrderCode(payload.orderCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found: " + payload.orderCode()));

        // Idempotency check: if this webhook idempotencyKey was already processed as SUCCESS, return immediately
        PaymentTransactionEntity existingTxn = paymentTransactionRepository
                .findByIdempotencyKey(payload.idempotencyKey())
                .orElse(null);

        if (existingTxn != null && "SUCCESS".equals(existingTxn.getStatus())) {
            log.info("Idempotent webhook skip: payment transaction {} already processed", payload.idempotencyKey());
            return toOrderDto(order);
        }

        boolean isSuccess = "SUCCESS".equalsIgnoreCase(payload.status());
        if (existingTxn == null) {
            existingTxn = PaymentTransactionEntity.builder()
                    .order(order)
                    .provider(provider.name())
                    .idempotencyKey(payload.idempotencyKey())
                    .externalTxnId(payload.externalTxnId())
                    .amount(payload.amount())
                    .status(isSuccess ? "SUCCESS" : "FAILED")
                    .rawWebhookPayload(Map.of(
                            "externalTxnId", payload.externalTxnId(),
                            "status", payload.status()
                    ))
                    .build();
        } else {
            existingTxn.setExternalTxnId(payload.externalTxnId());
            existingTxn.setStatus(isSuccess ? "SUCCESS" : "FAILED");
        }
        paymentTransactionRepository.save(existingTxn);

        if (isSuccess && order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getOrderStatus() == OrderStatus.PENDING) {
                order.setOrderStatus(OrderStatus.CONFIRMED);
            }
            order.setPaidAt(Instant.now());
            orderRepository.save(order);

            saveOutboxEvent(
                    "ORDER",
                    order.getOrderCode(),
                    "ORDER_PAID",
                    Map.of(
                            "orderCode", order.getOrderCode(),
                            "userId", order.getUser().getId().toString(),
                            "provider", provider.name(),
                            "amount", payload.amount()
                    )
            );
        } else if (!isSuccess && order.getPaymentStatus() == PaymentStatus.UNPAID) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
        }

        return toOrderDto(order);
    }

    /**
     * Cancels a PENDING/CONFIRMED order and atomically releases its reserved stock back to available_qty.
     */
    @Transactional
    public OrderResponseDto cancelOrderByPartner(UUID userId, String orderCode) {
        OrderEntity order = orderRepository.findWithDetailsByOrderCode(orderCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You can only cancel your own orders");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Only PENDING orders can be self-cancelled. Please contact B2B Support."
            );
        }

        releaseOrderStock(order);
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        orderRepository.save(order);

        return toOrderDto(order);
    }

    /**
     * Admin/Manager/Seller updates order status, attaches waybill tracking number, and handles stock lifecycle.
     */
    @Transactional
    public OrderResponseDto updateOrderStatusByAdmin(String orderCode, AdminUpdateOrderStatusRequestDto request) {
        OrderEntity order = orderRepository.findWithDetailsByOrderCode(orderCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus targetStatus = request.targetStatus();

        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot transition order from terminal status " + currentStatus);
        }

        if (targetStatus == OrderStatus.CANCELLED) {
            releaseOrderStock(order);
            order.setCancelledAt(Instant.now());
        } else if (targetStatus == OrderStatus.SHIPPED && currentStatus != OrderStatus.SHIPPED) {
            deductShippedOrderStock(order);
            order.setShippedAt(Instant.now());
        } else if (targetStatus == OrderStatus.DELIVERED || targetStatus == OrderStatus.COMPLETED) {
            order.setDeliveredAt(Instant.now());
        }

        if (request.trackingNumber() != null && !request.trackingNumber().isBlank()) {
            order.setTrackingNumber(request.trackingNumber().trim());
        }
        if (request.adminMemo() != null) {
            order.setAdminMemo(request.adminMemo());
        }
        order.setOrderStatus(targetStatus);
        orderRepository.save(order);

        saveOutboxEvent(
                "ORDER",
                order.getOrderCode(),
                "ORDER_STATUS_CHANGED",
                Map.of(
                        "orderCode", order.getOrderCode(),
                        "userId", order.getUser().getId().toString(),
                        "newStatus", targetStatus.name(),
                        "trackingNumber", order.getTrackingNumber() == null ? "" : order.getTrackingNumber()
                )
        );

        return toOrderDto(order);
    }

    /**
     * Admin bulk waybill import (replaces CSV/Excel bulk tracking upload in b2b-orders.php).
     */
    @Transactional
    public List<OrderResponseDto> bulkAssignWaybills(List<AdminBulkWaybillItemDto> waybills) {
        List<OrderResponseDto> updated = new ArrayList<>();
        for (AdminBulkWaybillItemDto item : waybills) {
            OrderEntity order = orderRepository.findWithDetailsByOrderCode(item.orderCode().trim())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found: " + item.orderCode()));

            if (order.getOrderStatus() != OrderStatus.SHIPPED
                    && order.getOrderStatus() != OrderStatus.DELIVERED
                    && order.getOrderStatus() != OrderStatus.COMPLETED
                    && order.getOrderStatus() != OrderStatus.CANCELLED) {
                deductShippedOrderStock(order);
                order.setOrderStatus(OrderStatus.SHIPPED);
                order.setShippedAt(Instant.now());
            }

            order.setTrackingNumber(item.trackingNumber().trim());
            if (item.shippingProvider() != null) {
                order.setShippingProvider(item.shippingProvider());
            }
            orderRepository.save(order);
            updated.add(toOrderDto(order));
        }
        return updated;
    }

    /**
     * Keyset-paginated Partner Order History (Tab 4 of b2b-portal.php).
     */
    @Transactional(readOnly = true)
    public CursorPageDto<OrderResponseDto> listPartnerOrdersKeyset(
            UUID userId,
            OrderType orderType,
            OrderStatus orderStatus,
            Instant cursorCreatedAt,
            UUID cursorId,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<OrderEntity> fetched = orderRepository.findPartnerOrdersKeyset(
                userId,
                orderType,
                orderStatus,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, safeLimit + 1)
        );
        return toCursorPage(fetched, safeLimit);
    }

    /**
     * Keyset-paginated Admin Order Management list (b2b-orders.php).
     */
    @Transactional(readOnly = true)
    public CursorPageDto<OrderResponseDto> listAdminOrdersKeyset(
            OrderType orderType,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            Instant cursorCreatedAt,
            UUID cursorId,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<OrderEntity> fetched = orderRepository.findAdminOrdersKeyset(
                orderType,
                orderStatus,
                paymentStatus,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, safeLimit + 1)
        );
        return toCursorPage(fetched, safeLimit);
    }

    /**
     * Tab 1 B2B Partner Dashboard Overview KPIs &amp; 4 most recent orders.
     */
    @Transactional(readOnly = true)
    public PartnerDashboardOverviewDto getPartnerDashboardOverview(UUID userId) {
        BigDecimal totalSpend = orderRepository.sumPaidSpendByUserId(userId);
        long totalOrders = orderRepository.countByUser_Id(userId);
        long activeOrders = orderRepository.countByUser_IdAndOrderStatusIn(
                userId,
                List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED)
        );
        long sampleOrders = orderRepository.countByUser_IdAndOrderType(userId, OrderType.SAMPLE);

        List<OrderResponseDto> recentOrders = orderRepository.findPartnerOrdersKeyset(
                userId, null, null, null, null, PageRequest.of(0, 4)
        ).stream().map(this::toOrderDto).toList();

        return PartnerDashboardOverviewDto.builder()
                .totalCumulativeSpend(totalSpend == null ? BigDecimal.ZERO : totalSpend)
                .totalOrdersCount(totalOrders)
                .activeInTransitOrdersCount(activeOrders)
                .sampleOrdersCount(sampleOrders)
                .recentOrders(recentOrders)
                .build();
    }

    /**
     * Admin Sales &amp; Analytics 6 KPI cards (b2b-analytics.php).
     */
    @Transactional(readOnly = true)
    public AdminSalesAnalyticsKpiDto getAdminSalesAnalyticsKpis() {
        BigDecimal totalRevenue = orderRepository.sumTotalPlatformRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        long totalOrders = orderRepository.count();
        long paidOrders = orderRepository.countByPaymentStatus(PaymentStatus.PAID);
        long completedOrders = orderRepository.countByOrderStatusIn(List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED));
        long cancelledOrders = orderRepository.countByOrderStatusIn(List.of(OrderStatus.CANCELLED));

        BigDecimal aov = paidOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal completionRate = totalOrders > 0
                ? BigDecimal.valueOf(completedOrders * 100.0 / totalOrders).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal cancellationRate = totalOrders > 0
                ? BigDecimal.valueOf(cancelledOrders * 100.0 / totalOrders).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return AdminSalesAnalyticsKpiDto.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(aov)
                .paidOrdersCount(paidOrders)
                .deliveryCompletionRatePercent(completionRate)
                .cancellationRatePercent(cancellationRate)
                .build();
    }

    private void releaseOrderStock(OrderEntity order) {
        if (order.getBranch() == null || order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        List<StockReservationItemDto> items = order.getItems().stream()
                .map(i -> StockReservationItemDto.builder()
                        .productId(i.getProduct().getId())
                        .quantity(i.getQuantity())
                        .build())
                .toList();
        inventoryService.releaseReservedStockForOrder(order.getBranch().getId(), items);
    }

    private void deductShippedOrderStock(OrderEntity order) {
        if (order.getBranch() == null || order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        List<StockReservationItemDto> items = order.getItems().stream()
                .map(i -> StockReservationItemDto.builder()
                        .productId(i.getProduct().getId())
                        .quantity(i.getQuantity())
                        .build())
                .toList();
        inventoryService.confirmShipmentDeduction(order.getBranch().getId(), items);
    }

    private void saveOutboxEvent(
            String aggregateType,
            String aggregateId,
            String eventType,
            Map<String, Object> payload
    ) {
        outboxEventRepository.save(OutboxEventEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status("PENDING")
                .retryCount(0)
                .build());
    }

    private String generateOrderCode(OrderType orderType) {
        String prefix = orderType == OrderType.SAMPLE ? "SM" : "UM";
        String datePart = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(ORDER_DATE_FMT);
        int randomSuffix = ThreadLocalRandom.current().nextInt(100000, 999999);
        return prefix + "-" + datePart + "-" + randomSuffix;
    }

    private CursorPageDto<OrderResponseDto> toCursorPage(List<OrderEntity> fetched, int safeLimit) {
        boolean hasNext = fetched.size() > safeLimit;
        List<OrderEntity> pageItems = hasNext ? fetched.subList(0, safeLimit) : fetched;
        if (pageItems.isEmpty()) {
            return CursorPageDto.empty();
        }
        OrderEntity last = pageItems.get(pageItems.size() - 1);
        List<OrderResponseDto> dtos = pageItems.stream().map(this::toOrderDto).toList();
        return new CursorPageDto<>(
                dtos,
                hasNext ? last.getCreatedAt() : null,
                hasNext ? last.getId() : null,
                hasNext
        );
    }

    public OrderResponseDto toOrderDto(OrderEntity order) {
        List<OrderItemResponseDto> itemDtos = order.getItems() == null ? List.of() : order.getItems().stream()
                .map(i -> OrderItemResponseDto.builder()
                        .id(i.getId())
                        .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                        .sku(i.getSkuSnapshot())
                        .productName(i.getProductNameSnap())
                        .unitLabel(i.getUnitLabelSnap())
                        .appliedTierCode(i.getAppliedTierCode())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .build())
                .toList();

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .branchId(order.getBranch() != null ? order.getBranch().getId() : null)
                .branchCode(order.getBranch() != null ? order.getBranch().getCode() : null)
                .branchName(order.getBranch() != null ? order.getBranch().getName() : null)
                .orderType(order.getOrderType())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .shippingProvider(order.getShippingProvider())
                .trackingNumber(order.getTrackingNumber())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingPostalCode(order.getShippingPostalCode())
                .shippingAddressLine(order.getShippingAddressLine())
                .shippingAddressDetail(order.getShippingAddressDetail())
                .orderNote(order.getOrderNote())
                .adminMemo(order.getAdminMemo())
                .items(itemDtos)
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
