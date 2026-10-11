package com.b2b.warehouse.gateway;

import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.gateway.payment.MomoPaymentGateway;
import com.b2b.warehouse.gateway.payment.PaymentGatewayRouter;
import com.b2b.warehouse.gateway.payment.PaymentInitiationDto;
import com.b2b.warehouse.gateway.payment.PaymentWebhookPayloadDto;
import com.b2b.warehouse.gateway.payment.VietQrPaymentGateway;
import com.b2b.warehouse.gateway.payment.VnPayPaymentGateway;
import com.b2b.warehouse.gateway.shipping.AhamoveShippingGateway;
import com.b2b.warehouse.gateway.shipping.GhnShippingGateway;
import com.b2b.warehouse.gateway.shipping.GhtkShippingGateway;
import com.b2b.warehouse.gateway.shipping.SelfPickupShippingGateway;
import com.b2b.warehouse.gateway.shipping.ShippingGatewayRouter;
import com.b2b.warehouse.gateway.shipping.ShippingQuoteDto;
import com.b2b.warehouse.order.entity.OrderEntity;
import com.b2b.warehouse.order.entity.PaymentMethod;
import com.b2b.warehouse.order.entity.ShippingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouterAndSignatureTest {

    private ShippingGatewayRouter shippingRouter;
    private VnPayPaymentGateway vnPayGateway;
    private MomoPaymentGateway momoGateway;
    private VietQrPaymentGateway vietQrGateway;
    private PaymentGatewayRouter paymentRouter;

    @BeforeEach
    void setUp() {
        shippingRouter = new ShippingGatewayRouter(List.of(
                new GhnShippingGateway(),
                new GhtkShippingGateway(),
                new AhamoveShippingGateway(),
                new SelfPickupShippingGateway()
        ));

        vnPayGateway = new VnPayPaymentGateway();
        ReflectionTestUtils.setField(vnPayGateway, "tmnCode", "DEMO_VNPAY_TMN");
        ReflectionTestUtils.setField(vnPayGateway, "hashSecret", "DEMO_VNPAY_SECRET_2026");
        ReflectionTestUtils.setField(vnPayGateway, "payUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");

        momoGateway = new MomoPaymentGateway();
        ReflectionTestUtils.setField(momoGateway, "partnerCode", "DEMO_MOMO");
        ReflectionTestUtils.setField(momoGateway, "secretKey", "DEMO_MOMO_SECRET_2026");
        ReflectionTestUtils.setField(momoGateway, "endpoint", "https://test-payment.momo.vn/v2/gateway/api/create");

        vietQrGateway = new VietQrPaymentGateway();
        ReflectionTestUtils.setField(vietQrGateway, "checksumKey", "DEMO_VIETQR_SECRET_2026");

        paymentRouter = new PaymentGatewayRouter(List.of(vnPayGateway, momoGateway, vietQrGateway));
    }

    @Test
    @DisplayName("GHN and GHTK should compute weight-based B2B parcel shipping fees accurately")
    void shouldCalculateWeightBasedShippingForGhnAndGhtk() {
        BranchEntity branch = BranchEntity.builder().latitude(10.7769).longitude(106.7009).build();

        // 3,000g = 2,000g base + 2 blocks of 500g
        ShippingQuoteDto ghnQuote = shippingRouter.calculateShippingQuote(ShippingProvider.GHN, branch, 3000, null, null);
        assertThat(ghnQuote.shippingFee()).isEqualByComparingTo("40000.00"); // 30,000 + 2 * 5,000

        ShippingQuoteDto ghtkQuote = shippingRouter.calculateShippingQuote(ShippingProvider.GHTK, branch, 3000, null, null);
        assertThat(ghtkQuote.shippingFee()).isEqualByComparingTo("33000.00"); // 25,000 + 2 * 4,000
    }

    @Test
    @DisplayName("AhaMove should compute GPS Haversine distance-based same-day delivery fee from origin branch to destination")
    void shouldCalculateGpsDistanceShippingForAhamove() {
        // Origin: District 1, HCMC (10.7769, 106.7009) -> Destination: District 7, HCMC (10.7285, 106.7218) ~ 5.8 km
        BranchEntity originBranch = BranchEntity.builder().latitude(10.7769).longitude(106.7009).build();

        ShippingQuoteDto ahamoveQuote = shippingRouter.calculateShippingQuote(
                ShippingProvider.AHAMOVE,
                originBranch,
                1500,
                10.7285,
                106.7218
        );

        assertThat(ahamoveQuote.distanceKm()).isBetween(5.0, 6.5);
        assertThat(ahamoveQuote.shippingFee()).isGreaterThan(new BigDecimal("25000.00"));
    }

    @Test
    @DisplayName("VNPay should generate HMAC-SHA512 signed redirect URL and verify authentic webhooks in constant time")
    void shouldInitiateAndVerifyVnPayHmacSha512Signature() {
        OrderEntity order = OrderEntity.builder()
                .orderCode("UM-261011-889900")
                .paymentMethod(PaymentMethod.VNPAY)
                .totalAmount(new BigDecimal("5740000.00"))
                .build();

        PaymentInitiationDto init = paymentRouter.initiatePayment(order);
        assertThat(init.paymentUrl()).contains("vnp_TxnRef=UM-261011-889900").contains("vnp_SecureHash=");

        PaymentWebhookPayloadDto unsignedPayload = PaymentWebhookPayloadDto.builder()
                .orderCode("UM-261011-889900")
                .externalTxnId("VNP14556677")
                .idempotencyKey("IDEMP-001")
                .amount(new BigDecimal("5740000.00"))
                .status("SUCCESS")
                .signature("")
                .build();
        String validSignature = vnPayGateway.computeWebhookSignatureForTest(unsignedPayload);

        PaymentWebhookPayloadDto validWebhook = PaymentWebhookPayloadDto.builder()
                .orderCode("UM-261011-889900")
                .externalTxnId("VNP14556677")
                .idempotencyKey("IDEMP-001")
                .amount(new BigDecimal("5740000.00"))
                .status("SUCCESS")
                .signature(validSignature)
                .build();

        assertThat(paymentRouter.verifyWebhook(PaymentMethod.VNPAY, validWebhook)).isTrue();

        PaymentWebhookPayloadDto forgedWebhook = PaymentWebhookPayloadDto.builder()
                .orderCode("UM-261011-889900")
                .externalTxnId("VNP14556677")
                .idempotencyKey("IDEMP-001")
                .amount(new BigDecimal("1000.00")) // Attacker modified amount
                .status("SUCCESS")
                .signature(validSignature)
                .build();

        assertThat(paymentRouter.verifyWebhook(PaymentMethod.VNPAY, forgedWebhook)).isFalse();
    }
}
