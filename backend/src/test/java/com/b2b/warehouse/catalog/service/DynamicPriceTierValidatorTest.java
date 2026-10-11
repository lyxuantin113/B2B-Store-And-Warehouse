package com.b2b.warehouse.catalog.service;

import com.b2b.warehouse.catalog.dto.PriceTierDto;
import com.b2b.warehouse.catalog.dto.WholesalePriceQuoteDto;
import com.b2b.warehouse.catalog.entity.ProductEntity;
import com.b2b.warehouse.catalog.entity.ProductPriceTierEntity;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicPriceTierValidatorTest {

    private final DynamicPriceTierValidator validator = new DynamicPriceTierValidator();

    @Test
    @DisplayName("Should accept arbitrary Admin-configured quantity intervals (e.g. 4 custom tiers: 1~4, 5~19, 20~99, 100+) and sort them")
    void shouldValidateAndSortArbitraryAdminConfiguredTiers() {
        BigDecimal retailPrice = new BigDecimal("1000000.00");

        // Admin submits 4 custom brackets out of order
        List<PriceTierDto> adminTiers = List.of(
                PriceTierDto.builder().minQty(100).maxQty(null).unitPrice(new BigDecimal("600000.00")).build(),
                PriceTierDto.builder().minQty(1).maxQty(4).unitPrice(new BigDecimal("850000.00")).build(),
                PriceTierDto.builder().minQty(20).maxQty(99).unitPrice(new BigDecimal("700000.00")).build(),
                PriceTierDto.builder().minQty(5).maxQty(19).unitPrice(new BigDecimal("780000.00")).build()
        );

        List<ProductPriceTierEntity> entities = validator.validateAndBuildEntities(adminTiers, retailPrice);

        assertThat(entities).hasSize(4);
        assertThat(entities.get(0).formatRangeLabel()).isEqualTo("1~4");
        assertThat(entities.get(0).getDiscountPercent()).isEqualByComparingTo("15.00");

        assertThat(entities.get(1).formatRangeLabel()).isEqualTo("5~19");
        assertThat(entities.get(1).getDiscountPercent()).isEqualByComparingTo("22.00");

        assertThat(entities.get(2).formatRangeLabel()).isEqualTo("20~99");
        assertThat(entities.get(2).getDiscountPercent()).isEqualByComparingTo("30.00");

        assertThat(entities.get(3).formatRangeLabel()).isEqualTo("100+");
        assertThat(entities.get(3).getDiscountPercent()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("Should dynamically resolve wholesale unit price and line total for any quantity based on Admin tiers")
    void shouldDynamicallyCalculateQuoteAcrossCustomAdminBrackets() {
        BigDecimal retailPrice = new BigDecimal("500000.00");
        List<ProductPriceTierEntity> tiers = validator.validateAndBuildEntities(
                List.of(
                        PriceTierDto.builder().tierCode("STARTER_PACK").minQty(1).maxQty(5).unitPrice(new BigDecimal("420000.00")).build(),
                        PriceTierDto.builder().tierCode("SALON_PACK").minQty(6).maxQty(25).unitPrice(new BigDecimal("380000.00")).build(),
                        PriceTierDto.builder().tierCode("DISTRIBUTOR_VIP").minQty(26).maxQty(null).unitPrice(new BigDecimal("320000.00")).build()
                ),
                retailPrice
        );

        ProductEntity product = ProductEntity.builder()
                .id(UUID.randomUUID())
                .sku("UM-SERUM-01")
                .name("Umego Intensive Repair Serum")
                .unitLabel("Box (20ea)")
                .retailPrice(retailPrice)
                .baseWholesalePrice(new BigDecimal("420000.00"))
                .priceTiers(tiers)
                .build();

        WholesalePriceQuoteDto quoteQty3 = validator.calculateQuote(product, 3);
        assertThat(quoteQty3.appliedTierCode()).isEqualTo("STARTER_PACK");
        assertThat(quoteQty3.appliedRangeLabel()).isEqualTo("1~5");
        assertThat(quoteQty3.appliedUnitPrice()).isEqualByComparingTo("420000.00");
        assertThat(quoteQty3.lineTotal()).isEqualByComparingTo("1260000.00");

        WholesalePriceQuoteDto quoteQty15 = validator.calculateQuote(product, 15);
        assertThat(quoteQty15.appliedTierCode()).isEqualTo("SALON_PACK");
        assertThat(quoteQty15.appliedRangeLabel()).isEqualTo("6~25");
        assertThat(quoteQty15.appliedUnitPrice()).isEqualByComparingTo("380000.00");
        assertThat(quoteQty15.lineTotal()).isEqualByComparingTo("5700000.00");

        WholesalePriceQuoteDto quoteQty40 = validator.calculateQuote(product, 40);
        assertThat(quoteQty40.appliedTierCode()).isEqualTo("DISTRIBUTOR_VIP");
        assertThat(quoteQty40.appliedRangeLabel()).isEqualTo("26+");
        assertThat(quoteQty40.appliedUnitPrice()).isEqualByComparingTo("320000.00");
        assertThat(quoteQty40.lineTotal()).isEqualByComparingTo("12800000.00");
    }

    @Test
    @DisplayName("Should reject overlapping quantity intervals configured by Admin (e.g. 1~6 and 5~12)")
    void shouldRejectOverlappingQuantityTiers() {
        List<PriceTierDto> overlappingTiers = List.of(
                PriceTierDto.builder().minQty(1).maxQty(6).unitPrice(new BigDecimal("400000.00")).build(),
                PriceTierDto.builder().minQty(5).maxQty(12).unitPrice(new BigDecimal("350000.00")).build()
        );

        assertThatThrownBy(() -> validator.validateAndBuildEntities(overlappingTiers, new BigDecimal("500000.00")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("Should reject open-ended tier (maxQty = null) if followed by another tier with higher minQty")
    void shouldRejectOpenEndedTierBeforeHigherTier() {
        List<PriceTierDto> invalidOpenEnded = List.of(
                PriceTierDto.builder().minQty(1).maxQty(null).unitPrice(new BigDecimal("400000.00")).build(),
                PriceTierDto.builder().minQty(10).maxQty(20).unitPrice(new BigDecimal("350000.00")).build()
        );

        assertThatThrownBy(() -> validator.validateAndBuildEntities(invalidOpenEnded, new BigDecimal("500000.00")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
