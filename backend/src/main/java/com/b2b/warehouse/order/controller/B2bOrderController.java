package com.b2b.warehouse.order.controller;

import com.b2b.warehouse.auth.security.AuthenticatedUser;
import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.common.dto.CursorPageDto;
import com.b2b.warehouse.order.dto.CreateOrderRequestDto;
import com.b2b.warehouse.order.dto.OrderCheckoutResponseDto;
import com.b2b.warehouse.order.dto.OrderResponseDto;
import com.b2b.warehouse.order.dto.PartnerDashboardOverviewDto;
import com.b2b.warehouse.order.entity.OrderStatus;
import com.b2b.warehouse.order.entity.OrderType;
import com.b2b.warehouse.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/b2b")
@RequiredArgsConstructor
@PreAuthorize("hasRole('B2B_PARTNER')")
@Tag(name = "09. B2B Partner Orders & Checkout", description = "Tab 1 Overview KPIs, Tab 2 Wholesale Checkout, Tab 4 Order History & Tab 7 Free Sample Checkout")
public class B2bOrderController {

    private final OrderService orderService;

    @GetMapping("/dashboard/overview")
    @Operation(summary = "Tab 1 Dashboard Overview: Get 4 KPI cards and 4 most recent orders")
    public ApiResponse<PartnerDashboardOverviewDto> getDashboardOverview(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.ok(orderService.getPartnerDashboardOverview(currentUser.getUserId()));
    }

    @PostMapping("/orders/wholesale")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('order:write')")
    @Operation(summary = "Tab 2 One-Page Checkout: Place a B2B Wholesale Order using Admin-configured dynamic price tiers")
    public ApiResponse<OrderCheckoutResponseDto> placeWholesaleOrder(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateOrderRequestDto request
    ) {
        return ApiResponse.ok(
                orderService.placeWholesaleOrder(currentUser.getUserId(), request),
                "Wholesale order created and inventory reserved"
        );
    }

    @PostMapping("/orders/sample")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('order:write')")
    @Operation(summary = "Tab 7 Free Sample Request: Place a Sample Order (Product cost = 0 VND; Partner pays shipping fee only)")
    public ApiResponse<OrderCheckoutResponseDto> placeSampleOrder(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateOrderRequestDto request
    ) {
        return ApiResponse.ok(
                orderService.placeSampleOrder(currentUser.getUserId(), request),
                "Free sample order created (Product price = 0 VND; shipping fee applied)"
        );
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('order:read')")
    @Operation(summary = "Tab 4 Order History: Keyset-paginated list of partner's WHOLESALE and SAMPLE orders")
    public ApiResponse<CursorPageDto<OrderResponseDto>> listMyOrders(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) OrderType orderType,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) Instant cursorCreatedAt,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(orderService.listPartnerOrdersKeyset(
                currentUser.getUserId(),
                orderType,
                orderStatus,
                cursorCreatedAt,
                cursorId,
                limit
        ));
    }

    @PostMapping("/orders/{orderCode}/cancel")
    @PreAuthorize("hasAuthority('order:write')")
    @Operation(summary = "Cancel a PENDING order and release reserved warehouse stock")
    public ApiResponse<OrderResponseDto> cancelMyOrder(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String orderCode
    ) {
        return ApiResponse.ok(
                orderService.cancelOrderByPartner(currentUser.getUserId(), orderCode),
                "Order cancelled and reserved stock released"
        );
    }
}
