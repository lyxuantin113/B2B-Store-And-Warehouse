package com.b2b.warehouse.order.service;

import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.auth.repository.UserRepository;
import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.branch.repository.BranchRepository;
import com.b2b.warehouse.catalog.dto.PriceTierDto;
import com.b2b.warehouse.catalog.entity.ProductEntity;
import com.b2b.warehouse.catalog.entity.ProductPriceTierEntity;
import com.b2b.warehouse.catalog.repository.ProductRepository;
import com.b2b.warehouse.catalog.service.DynamicPriceTierValidator;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.gateway.payment.MomoPaymentGateway;
import com.b2b.warehouse.gateway.payment.PaymentGatewayRouter;
import com.b2b.warehouse.gateway.payment.PaymentWebhookPayloadDto;
import com.b2b.warehouse.gateway.payment.VietQrPaymentGateway;
import com.b2b.warehouse.gateway.payment.VnPayPaymentGateway;
import com.b2b.warehouse.gateway.shipping.AhamoveShippingGateway;
import com.b2b.warehouse.gateway.shipping.GhnShippingGateway;
import com.b2b.warehouse.gateway.shipping.GhtkShippingGateway;
import com.b2b.warehouse.gateway.shipping.SelfPickupShippingGateway;
import com.b2b.warehouse.gateway.shipping.ShippingGatewayRouter;
import com.b2b.warehouse.inventory.service.InventoryService;
import com.b2b.warehouse.order.dto.CreateOrderRequestDto;
import com.b2b.warehouse.order.dto.OrderCheckoutResponseDto;
import com.b2b.warehouse.order.dto.OrderLineItemRequestDto;
import com.b2b.warehouse.order.dto.OrderResponseDto;
import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.OrderStatus;
import com.b2b.warehouse.order.entity.OrderType;
import com.b2b.warehouse.order.entity.PaymentMethod;
import com.b2b.warehouse.order.entity.PaymentStatus;
import com.b2b.warehouse.order.entity.PaymentTransactionEntity;
import com.b2b.warehouse.order.entity.ShippingProvider;
import com.b2b.warehouse.order.repository.OrderRepository;
import com.b2b.warehouse.order.repository.PaymentTransactionRepository;
import com.b2b.warehouse.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryService inventoryService;

    private DynamicPriceTierValidator priceTierValidator;
    private VnPayPaymentGateway vnPayGateway;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        priceTierValidator = new DynamicPriceTierValidator();

        ShippingGatewayRouter shippingRouter = new ShippingGatewayRouter(List.of(
                new GhnShippingGateway(),
                new GhtkShippingGateway(),
                new AhamoveShippingGateway(),
                new SelfPickupShippingGateway()
        ));

        vnPayGateway = new VnPayPaymentGateway();
        ReflectionTestUtils.setField(vnPayGateway, "tmnCode", "DEMO_VNPAY_TMN");
        ReflectionTestUtils.setField(vnPayGateway, "hashSecret", "DEMO_VNPAY_SECRET_2026");
        ReflectionTestUtils.setField(vnPayGateway, "payUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");

        MomoPaymentGateway momoGateway = new MomoPaymentGateway();
        ReflectionTestUtils.setField(momoGateway, "partnerCode", "DEMO_MOMO");
        ReflectionTestUtils.setField(momoGateway, "secretKey", "DEMO_MOMO_SECRET_2026");
        ReflectionTestUtils.setField(momoGateway, "endpoint", "https://test-payment.momo.vn/v2/gateway/api/create");

        VietQrPaymentGateway vietQrGateway = new VietQrPaymentGateway();
        ReflectionTestUtils.setField(vietQrGateway, "checksumKey", "DEMO_VIETQR_SECRET_2026");

        PaymentGatewayRouter paymentRouter = new PaymentGatewayRouter(List.of(vnPayGateway, momoGateway, vietQrGateway));

        orderService = new OrderService(
                orderRepository,
                paymentTransactionRepository,
                outboxEventRepository,
                userRepository,
                branchRepository,
                productRepository,
                priceTierValidator,
                inventoryService,
                shippingRouter,
                paymentRouter
        );
    }

    @Test
    @DisplayName("Tab 2 Wholesale Checkout: Should apply Admin-configured dynamic price tier, reserve stock, add GHN shipping fee, and write Outbox event")
    void shouldPlaceWholesaleOrderUsingDynamicAdminPriceTiers() {
        UUID userId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        UserEntity user = UserEntity.builder().id(userId).email("partner@umego.vn").accountStatus(AccountStatus.ACTIVE).build();
        BranchEntity branch = BranchEntity.builder().id(branchId).code("HCMC-01").name("HCMC Hub").isActive(true).build();

        // Admin configured custom brackets: 1~5 -> 420k, 6~25 -> 380k, 26+ -> 320k
        List<ProductPriceTierEntity> dynamicTiers = priceTierValidator.validateAndBuildEntities(
                List.of(
                        PriceTierDto.builder().tierCode("TIER_1_5").minQty(1).maxQty(5).unitPrice(new BigDecimal("420000.00")).build(),
                        PriceTierDto.builder().tierCode("TIER_6_25").minQty(6).maxQty(25).unitPrice(new BigDecimal("380000.00")).build(),
                        PriceTierDto.builder().tierCode("TIER_26_PLUS").minQty(26).maxQty(null).unitPrice(new BigDecimal("320000.00")).build()
                ),
                new BigDecimal("500000.00")
        );

        ProductEntity product = ProductEntity.builder()
                .id(productId)
                .sku("UM-SERUM-01")
                .name("Umego Serum Box")
                .unitLabel("Box (20ea)")
                .retailPrice(new BigDecimal("500000.00"))
                .baseWholesalePrice(new BigDecimal("420000.00"))
                .weightGrams(200) // 10 boxes * 200g = 2,000g -> GHN base fee 30,000 VND
                .priceTiers(dynamicTiers)
                .build();

        when(userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(productRepository.findAllWithDetailsByIdInAndDeletedAtIsNull(Set.of(productId))).thenReturn(List.of(product));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                .branchId(branchId)
                .paymentMethod(PaymentMethod.VNPAY)
                .shippingProvider(ShippingProvider.GHN)
                .recipientName("Purchasing Dept")
                .recipientPhone("0901234567")
                .shippingAddressLine("123 Nguyen Hue, District 1, HCMC")
                .items(List.of(OrderLineItemRequestDto.builder().productId(productId).quantity(10).build()))
                .build();

        OrderCheckoutResponseDto response = orderService.placeWholesaleOrder(userId, request);

        // 10 boxes matches TIER_6_25 -> 380,000 * 10 = 3,800,000 VND subtotal
        assertThat(response.order().orderType()).isEqualTo(OrderType.WHOLESALE);
        assertThat(response.order().subtotalAmount()).isEqualByComparingTo("3800000.00");
        assertThat(response.order().discountAmount()).isEqualByComparingTo("1200000.00"); // (500k - 380k) * 10
        assertThat(response.order().shippingFee()).isEqualByComparingTo("30000.00"); // GHN 2,000g base fee
        assertThat(response.order().totalAmount()).isEqualByComparingTo("3830000.00");
        assertThat(response.order().items().get(0).appliedTierCode()).isEqualTo("TIER_6_25");

        verify(inventoryService).reserveStockForOrder(eq(branchId), any());
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Tab 7 Free Sample Checkout: Product price should be 0 VND and Partner pays only the shipping fee")
    void shouldPlaceSampleOrderWithZeroProductCostAndShippingFeeOnly() {
        UUID userId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        UserEntity user = UserEntity.builder().id(userId).email("partner@umego.vn").accountStatus(AccountStatus.ACTIVE).build();
        BranchEntity branch = BranchEntity.builder().id(branchId).code("HCMC-01").name("HCMC Hub").isActive(true).build();

        ProductEntity sampleProduct = ProductEntity.builder()
                .id(productId)
                .sku("UM-MASK-SAMPLE")
                .name("Umego Hydro Mask Sample")
                .unitLabel("Sample Pack")
                .retailPrice(new BigDecimal("300000.00"))
                .baseWholesalePrice(new BigDecimal("240000.00"))
                .sampleEnabled(true)
                .sampleMaxQty(2)
                .weightGrams(500)
                .build();

        when(userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(productRepository.findAllWithDetailsByIdInAndDeletedAtIsNull(Set.of(productId))).thenReturn(List.of(sampleProduct));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                .branchId(branchId)
                .paymentMethod(PaymentMethod.VIETQR)
                .shippingProvider(ShippingProvider.GHTK)
                .recipientName("Sample Receiver")
                .recipientPhone("0909998887")
                .shippingAddressLine("456 Le Loi, District 1, HCMC")
                .items(List.of(OrderLineItemRequestDto.builder().productId(productId).quantity(2).build()))
                .build();

        OrderCheckoutResponseDto response = orderService.placeSampleOrder(userId, request);

        assertThat(response.order().orderType()).isEqualTo(OrderType.SAMPLE);
        assertThat(response.order().subtotalAmount()).isEqualByComparingTo("0.00");
        assertThat(response.order().shippingFee()).isEqualByComparingTo("25000.00"); // GHTK base fee
        assertThat(response.order().totalAmount()).isEqualByComparingTo("25000.00"); // Partner pays shipping only!
        assertThat(response.order().items().get(0).unitPrice()).isEqualByComparingTo("0.00");
        assertThat(response.order().items().get(0).appliedTierCode()).isEqualTo("FREE_SAMPLE");
    }

    @Test
    @DisplayName("Tab 7 Free Sample Checkout: Should throw SAMPLE_QUOTA_EXCEEDED if requested quantity exceeds sampleMaxQty")
    void shouldRejectSampleOrderExceedingSampleMaxQty() {
        UUID userId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        UserEntity user = UserEntity.builder().id(userId).accountStatus(AccountStatus.ACTIVE).build();
        BranchEntity branch = BranchEntity.builder().id(branchId).isActive(true).build();
        ProductEntity sampleProduct = ProductEntity.builder()
                .id(productId)
                .sku("UM-MASK-SAMPLE")
                .sampleEnabled(true)
                .sampleMaxQty(2)
                .build();

        when(userRepository.findWithRoleByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(productRepository.findAllWithDetailsByIdInAndDeletedAtIsNull(Set.of(productId))).thenReturn(List.of(sampleProduct));

        CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                .branchId(branchId)
                .paymentMethod(PaymentMethod.VNPAY)
                .shippingProvider(ShippingProvider.GHN)
                .recipientName("Sample Receiver")
                .recipientPhone("0909998887")
                .shippingAddressLine("456 Le Loi")
                .items(List.of(OrderLineItemRequestDto.builder().productId(productId).quantity(5).build())) // > max 2
                .build();

        assertThatThrownBy(() -> orderService.placeSampleOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAMPLE_QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("Payment Webhook should mark order PAID & CONFIRMED and ignore duplicate idempotent retries")
    void shouldProcessWebhookIdempotently() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("partner@umego.vn").build();
        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderCode("UM-261011-112233")
                .user(user)
                .orderType(OrderType.WHOLESALE)
                .orderStatus(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(new BigDecimal("3830000.00"))
                .build();

        PaymentWebhookPayloadDto unsigned = PaymentWebhookPayloadDto.builder()
                .orderCode("UM-261011-112233")
                .externalTxnId("VNP-998877")
                .idempotencyKey("IDEMP-VNP-112233")
                .amount(new BigDecimal("3830000.00"))
                .status("SUCCESS")
                .signature("")
                .build();
        String validSig = vnPayGateway.computeWebhookSignatureForTest(unsigned);

        PaymentWebhookPayloadDto signedWebhook = PaymentWebhookPayloadDto.builder()
                .orderCode("UM-261011-112233")
                .externalTxnId("VNP-998877")
                .idempotencyKey("IDEMP-VNP-112233")
                .amount(new BigDecimal("3830000.00"))
                .status("SUCCESS")
                .signature(validSig)
                .build();

        when(orderRepository.findWithDetailsByOrderCode("UM-261011-112233")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByIdempotencyKey("IDEMP-VNP-112233")).thenReturn(Optional.empty());

        OrderResponseDto firstCall = orderService.handlePaymentWebhook(PaymentMethod.VNPAY, signedWebhook);

        assertThat(firstCall.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(firstCall.orderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(firstCall.paidAt()).isNotNull();

        // Simulate duplicate webhook retry from VNPay with the same idempotencyKey
        PaymentTransactionEntity alreadySucceeded = PaymentTransactionEntity.builder()
                .idempotencyKey("IDEMP-VNP-112233")
                .status("SUCCESS")
                .build();
        when(paymentTransactionRepository.findByIdempotencyKey("IDEMP-VNP-112233")).thenReturn(Optional.of(alreadySucceeded));

        OrderResponseDto duplicateRetry = orderService.handlePaymentWebhook(PaymentMethod.VNPAY, signedWebhook);
        assertThat(duplicateRetry.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(orderRepository, org.mockito.Mockito.times(1)).save(order); // Saved only once!
    }
}
