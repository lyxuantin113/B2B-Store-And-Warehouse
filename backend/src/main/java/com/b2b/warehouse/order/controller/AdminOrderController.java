package com.b2b.warehouse.order.controller;

import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.common.dto.CursorPageDto;
import com.b2b.warehouse.order.dto.AdminBulkWaybillItemDto;
import com.b2b.warehouse.order.dto.AdminSalesAnalyticsKpiDto;
import com.b2b.warehouse.order.dto.AdminUpdateOrderStatusRequestDto;
import com.b2b.warehouse.order.dto.OrderResponseDto;
import com.b2b.warehouse.order.entity.OrderStatus;
import com.b2b.warehouse.order.entity.OrderType;
import com.b2b.warehouse.order.entity.PaymentStatus;
import com.b2b.warehouse.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "10. Admin Order Fulfillment & Sales Analytics", description = "Admin Order Management, 6 Sales KPIs & Bulk Waybill Tracking Import")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/analytics/kpis")
    @PreAuthorize("hasAuthority('order:read')")
    @Operation(summary = "Get 6 Sales & Order KPI cards for Admin Analytics Dashboard (b2b-analytics.php)")
    public ApiResponse<AdminSalesAnalyticsKpiDto> getSalesAnalyticsKpis() {
        return ApiResponse.ok(orderService.getAdminSalesAnalyticsKpis());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order:read')")
    @Operation(summary = "Keyset-paginated Admin order list filtered by orderType, orderStatus, and paymentStatus")
    public ApiResponse<CursorPageDto<OrderResponseDto>> listOrders(
            @RequestParam(required = false) OrderType orderType,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) Instant cursorCreatedAt,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(orderService.listAdminOrdersKeyset(
                orderType,
                orderStatus,
                paymentStatus,
                cursorCreatedAt,
                cursorId,
                limit
        ));
    }

    @PatchMapping("/{orderCode}/status")
    @PreAuthorize("hasAuthority('order:manage')")
    @Operation(summary = "Update order status, attach waybill tracking number, and manage warehouse stock reservation")
    public ApiResponse<OrderResponseDto> updateOrderStatus(
            @PathVariable String orderCode,
            @Valid @RequestBody AdminUpdateOrderStatusRequestDto request
    ) {
        return ApiResponse.ok(
                orderService.updateOrderStatusByAdmin(orderCode, request),
                "Order " + orderCode + " transitioned to " + request.targetStatus()
        );
    }

    @PostMapping("/bulk-waybill")
    @PreAuthorize("hasAuthority('order:manage')")
    @Operation(summary = "Bulk assign GHN/GHTK/AhaMove waybill tracking numbers and mark orders as SHIPPED")
    public ApiResponse<List<OrderResponseDto>> bulkAssignWaybills(
            @Valid @RequestBody List<AdminBulkWaybillItemDto> waybills
    ) {
        List<OrderResponseDto> updated = orderService.bulkAssignWaybills(waybills);
        return ApiResponse.ok(updated, "Assigned tracking numbers and shipped " + updated.size() + " orders");
    }
}
