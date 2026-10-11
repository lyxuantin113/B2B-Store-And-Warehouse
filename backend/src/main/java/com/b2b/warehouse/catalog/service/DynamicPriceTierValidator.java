package com.b2b.warehouse.catalog.service;

import com.b2b.warehouse.catalog.dto.PriceTierDto;
import com.b2b.warehouse.catalog.dto.WholesalePriceQuoteDto;
import com.b2b.warehouse.catalog.entity.ProductEntity;
import com.b2b.warehouse.catalog.entity.ProductPriceTierEntity;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Validates and evaluates Admin-configured dynamic quantity price tiers.
 *
 * <p><b>Key Design Rule</b>: Quantity brackets ({@code minQty} ~ {@code maxQty}) are <b>100% Admin-configurable</b>
 * per product (never hardcoded). An Admin can define 1, 2, 3, 5, or N brackets with arbitrary boundaries
 * (e.g., {@code 1~4}, {@code 5~19}, {@code 20~99}, {@code 100+}).</p>
 */
@Component
public class DynamicPriceTierValidator {

    /**
     * Validates a list of Admin-submitted price tiers and converts them into sorted {@link ProductPriceTierEntity} instances.
     *
     * <ul>
     *   <li>Ensures each tier has {@code minQty >= 1} and {@code maxQty == null || maxQty >= minQty}.</li>
     *   <li>Sorts tiers by {@code minQty ASC} and verifies there are <b>no overlapping intervals</b>.</li>
     *   <li>Ensures an open-ended bracket ({@code maxQty == null}) only appears as the last (highest) tier.</li>
     *   <li>Auto-generates {@code tierCode} (e.g. {@code TIER_1_5}, {@code TIER_21_PLUS}) and {@code discountPercent}
     *       relative to {@code retailPrice} if not explicitly supplied by Admin.</li>
     * </ul>
     */
    public List<ProductPriceTierEntity> validateAndBuildEntities(
            List<PriceTierDto> requestedTiers,
            BigDecimal retailPrice
    ) {
        if (requestedTiers == null || requestedTiers.isEmpty()) {
            return List.of();
        }

        for (PriceTierDto dto : requestedTiers) {
            if (dto.minQty() == null || dto.minQty() < 1) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Price tier minQty must be >= 1");
            }
            if (dto.maxQty() != null && dto.maxQty() < dto.minQty()) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Price tier maxQty (" + dto.maxQty() + ") cannot be less than minQty (" + dto.minQty() + ")"
                );
            }
            if (dto.unitPrice() == null || dto.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Price tier unitPrice must be >= 0");
            }
        }

        List<PriceTierDto> sorted = new ArrayList<>(requestedTiers);
        sorted.sort(Comparator.comparingInt(PriceTierDto::minQty));

        List<ProductPriceTierEntity> entities = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            PriceTierDto current = sorted.get(i);

            if (i > 0) {
                PriceTierDto previous = sorted.get(i - 1);
                if (previous.maxQty() == null) {
                    throw new BusinessException(
                            ErrorCode.VALIDATION_ERROR,
                            "Open-ended price tier (" + previous.minQty() + "+) must be the highest quantity bracket and cannot be followed by another tier (" + current.minQty() + ")"
                    );
                }
                if (current.minQty() <= previous.maxQty()) {
                    throw new BusinessException(
                            ErrorCode.VALIDATION_ERROR,
                            "Overlapping quantity price tiers detected: [" + previous.minQty() + "~" + previous.maxQty()
                                    + "] overlaps with [" + current.minQty() + "~" + (current.maxQty() == null ? "+" : current.maxQty()) + "]"
                    );
                }
            }

            String resolvedTierCode = resolveTierCode(current);
            BigDecimal resolvedDiscount = resolveDiscountPercent(current.discountPercent(), current.unitPrice(), retailPrice);

            entities.add(ProductPriceTierEntity.builder()
                    .tierCode(resolvedTierCode)
                    .minQty(current.minQty())
                    .maxQty(current.maxQty())
                    .unitPrice(current.unitPrice().setScale(2, RoundingMode.HALF_UP))
                    .discountPercent(resolvedDiscount)
                    .build());
        }

        return entities;
    }

    /**
     * Dynamically resolves the applicable wholesale unit price and tier metadata for a given order quantity.
     *
     * <ol>
     *   <li>First checks for an exact bracket match where {@code minQty <= quantity <= maxQty} (or {@code maxQty == null}).</li>
     *   <li>If the Admin configured non-contiguous brackets with a gap (e.g. {@code 1~5} and {@code 10+}) and {@code quantity = 7},
     *       falls back to the highest bracket whose {@code minQty <= quantity}.</li>
     *   <li>If no bracket matches (or the product has no tiers), falls back to {@code product.baseWholesalePrice}.</li>
     * </ol>
     */
    public WholesalePriceQuoteDto calculateQuote(ProductEntity product, int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order quantity must be at least 1");
        }

        List<ProductPriceTierEntity> tiers = product.getPriceTiers();
        ProductPriceTierEntity matchedTier = null;

        if (tiers != null && !tiers.isEmpty()) {
            List<ProductPriceTierEntity> sortedTiers = tiers.stream()
                    .sorted(Comparator.comparingInt(ProductPriceTierEntity::getMinQty))
                    .toList();

            for (ProductPriceTierEntity tier : sortedTiers) {
                if (tier.matchesQuantity(quantity)) {
                    matchedTier = tier;
                    break;
                }
            }

            // Fallback for gaps between Admin-configured brackets: pick highest bracket where minQty <= quantity
            if (matchedTier == null) {
                for (ProductPriceTierEntity tier : sortedTiers) {
                    if (tier.getMinQty() <= quantity) {
                        matchedTier = tier;
                    }
                }
            }
        }

        BigDecimal appliedUnitPrice = matchedTier != null
                ? matchedTier.getUnitPrice()
                : product.getBaseWholesalePrice();
        String appliedTierCode = matchedTier != null ? matchedTier.getTierCode() : "BASE_WHOLESALE";
        String appliedRangeLabel = matchedTier != null ? matchedTier.formatRangeLabel() : "1+";

        BigDecimal qtyBd = BigDecimal.valueOf(quantity);
        BigDecimal lineTotal = appliedUnitPrice.multiply(qtyBd).setScale(2, RoundingMode.HALF_UP);
        BigDecimal retailLineTotal = product.getRetailPrice().multiply(qtyBd).setScale(2, RoundingMode.HALF_UP);
        BigDecimal savings = retailLineTotal.subtract(lineTotal).max(BigDecimal.ZERO);

        return WholesalePriceQuoteDto.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .productName(product.getName())
                .unitLabel(product.getUnitLabel())
                .quantity(quantity)
                .appliedTierCode(appliedTierCode)
                .appliedRangeLabel(appliedRangeLabel)
                .retailPrice(product.getRetailPrice())
                .baseWholesalePrice(product.getBaseWholesalePrice())
                .appliedUnitPrice(appliedUnitPrice)
                .lineTotal(lineTotal)
                .totalSavingsVsRetail(savings)
                .build();
    }

    private String resolveTierCode(PriceTierDto dto) {
        if (dto.tierCode() != null && !dto.tierCode().isBlank()) {
            return dto.tierCode().trim();
        }
        return dto.maxQty() == null
                ? "TIER_" + dto.minQty() + "_PLUS"
                : "TIER_" + dto.minQty() + "_" + dto.maxQty();
    }

    private BigDecimal resolveDiscountPercent(
            BigDecimal explicitDiscount,
            BigDecimal unitPrice,
            BigDecimal retailPrice
    ) {
        if (explicitDiscount != null) {
            return explicitDiscount.setScale(2, RoundingMode.HALF_UP);
        }
        if (retailPrice == null || retailPrice.compareTo(BigDecimal.ZERO) <= 0 || unitPrice.compareTo(retailPrice) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal diff = retailPrice.subtract(unitPrice);
        return diff.multiply(BigDecimal.valueOf(100))
                .divide(retailPrice, 2, RoundingMode.HALF_UP);
    }
}
