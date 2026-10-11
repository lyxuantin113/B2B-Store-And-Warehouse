package com.b2b.warehouse.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Dynamic Admin-configured wholesale price tier for a product.
 * Quantity ranges ({@code minQty} ~ {@code maxQty}) and number of tiers are 100% configurable by Admin per product.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_price_tiers")
public class ProductPriceTierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "tier_code", nullable = false, length = 60)
    private String tierCode;

    @Column(name = "min_qty", nullable = false)
    private Integer minQty;

    /**
     * Inclusive upper bound of this quantity bracket.
     * {@code null} indicates an open-ended bracket (e.g., {@code >= minQty}).
     */
    @Column(name = "max_qty")
    private Integer maxQty;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    public boolean matchesQuantity(int quantity) {
        if (minQty == null || quantity < minQty) {
            return false;
        }
        return maxQty == null || quantity <= maxQty;
    }

    public String formatRangeLabel() {
        return maxQty == null ? (minQty + "+") : (minQty + "~" + maxQty);
    }
}
