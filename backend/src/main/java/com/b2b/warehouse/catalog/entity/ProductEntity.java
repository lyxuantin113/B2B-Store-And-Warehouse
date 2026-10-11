package com.b2b.warehouse.catalog.entity;

import com.b2b.warehouse.common.entity.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class ProductEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "sku", nullable = false, unique = true, length = 60)
    private String sku;

    @Column(name = "slug", nullable = false, unique = true, length = 180)
    private String slug;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "unit_label", nullable = false, length = 40)
    private String unitLabel = "Box (20ea)";

    @Builder.Default
    @Column(name = "units_per_box", nullable = false)
    private Integer unitsPerBox = 20;

    @Column(name = "retail_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "base_wholesale_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseWholesalePrice;

    @Builder.Default
    @Column(name = "sample_enabled", nullable = false)
    private Boolean sampleEnabled = true;

    @Builder.Default
    @Column(name = "sample_max_qty", nullable = false)
    private Integer sampleMaxQty = 2;

    @Builder.Default
    @Column(name = "weight_grams", nullable = false)
    private Integer weightGrams = 500;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gallery_urls", columnDefinition = "jsonb")
    private List<String> galleryUrls = new ArrayList<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specs_json", columnDefinition = "jsonb")
    private Map<String, Object> specsJson = Map.of();

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("minQty ASC")
    private List<ProductPriceTierEntity> priceTiers = new ArrayList<>();

    public void replacePriceTiers(List<ProductPriceTierEntity> newTiers) {
        this.priceTiers.clear();
        if (newTiers != null) {
            for (ProductPriceTierEntity tier : newTiers) {
                tier.setProduct(this);
                this.priceTiers.add(tier);
            }
        }
    }
}
