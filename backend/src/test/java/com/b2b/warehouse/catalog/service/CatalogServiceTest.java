package com.b2b.warehouse.catalog.service;

import com.b2b.warehouse.catalog.dto.B2bProductResponseDto;
import com.b2b.warehouse.catalog.dto.PriceTierDto;
import com.b2b.warehouse.catalog.dto.UpsertProductRequestDto;
import com.b2b.warehouse.catalog.entity.CategoryEntity;
import com.b2b.warehouse.catalog.entity.ProductEntity;
import com.b2b.warehouse.catalog.repository.CategoryRepository;
import com.b2b.warehouse.catalog.repository.ProductRepository;
import com.b2b.warehouse.common.dto.CursorPageDto;
import com.b2b.warehouse.inventory.repository.BranchInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BranchInventoryRepository inventoryRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(
                categoryRepository,
                productRepository,
                inventoryRepository,
                new DynamicPriceTierValidator()
        );
    }

    @Test
    @DisplayName("Should allow Admin to create a product with custom quantity tiers and later reconfigure those tiers dynamically")
    void shouldCreateProductAndReconfigureDynamicPriceTiers() {
        CategoryEntity category = CategoryEntity.builder().id(1L).name("Skincare").slug("skincare").build();
        UUID productId = UUID.randomUUID();

        UpsertProductRequestDto createReq = UpsertProductRequestDto.builder()
                .categoryId(1L)
                .sku("UM-CREAM-01")
                .slug("um-cream-01")
                .name("Umego Barrier Cream")
                .unitLabel("Box (30ea)")
                .unitsPerBox(30)
                .retailPrice(new BigDecimal("1200000.00"))
                .baseWholesalePrice(new BigDecimal("1000000.00"))
                .priceTiers(List.of(
                        PriceTierDto.builder().minQty(1).maxQty(10).unitPrice(new BigDecimal("1000000.00")).build(),
                        PriceTierDto.builder().minQty(11).maxQty(null).unitPrice(new BigDecimal("850000.00")).build()
                ))
                .build();

        when(productRepository.existsBySkuIgnoreCase("UM-CREAM-01")).thenReturn(false);
        when(productRepository.existsBySlugIgnoreCase("um-cream-01")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> {
            ProductEntity p = inv.getArgument(0);
            p.setId(productId);
            return p;
        });

        B2bProductResponseDto created = catalogService.createProduct(createReq);

        assertThat(created.priceTiers()).hasSize(2);
        assertThat(created.priceTiers().get(0).rangeLabel()).isEqualTo("1~10");
        assertThat(created.priceTiers().get(1).rangeLabel()).isEqualTo("11+");

        // Now Admin reconfigures the product into 4 completely different quantity brackets: 1~2, 3~8, 9~24, 25+
        ProductEntity existingEntity = ProductEntity.builder()
                .id(productId)
                .category(category)
                .sku("UM-CREAM-01")
                .slug("um-cream-01")
                .name("Umego Barrier Cream")
                .unitLabel("Box (30ea)")
                .unitsPerBox(30)
                .retailPrice(new BigDecimal("1200000.00"))
                .baseWholesalePrice(new BigDecimal("1000000.00"))
                .build();

        when(productRepository.findWithDetailsByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(existingEntity));
        when(inventoryRepository.sumAvailableStockByProductIds(List.of(productId))).thenReturn(List.of());

        List<PriceTierDto> newDynamicBrackets = List.of(
                PriceTierDto.builder().minQty(1).maxQty(2).unitPrice(new BigDecimal("1020000.00")).build(),
                PriceTierDto.builder().minQty(3).maxQty(8).unitPrice(new BigDecimal("940000.00")).build(),
                PriceTierDto.builder().minQty(9).maxQty(24).unitPrice(new BigDecimal("860000.00")).build(),
                PriceTierDto.builder().minQty(25).maxQty(null).unitPrice(new BigDecimal("780000.00")).build()
        );

        B2bProductResponseDto reconfigured = catalogService.replaceProductPriceTiers(productId, newDynamicBrackets);

        assertThat(reconfigured.priceTiers()).hasSize(4);
        assertThat(reconfigured.priceTiers()).extracting(PriceTierDto::rangeLabel)
                .containsExactly("1~2", "3~8", "9~24", "25+");
    }

    @Test
    @DisplayName("Keyset B2B Catalog pagination should batch-load stock totals and return next cursor when hasNext=true")
    void shouldListB2bProductsWithKeysetPaginationAndBatchStock() {
        CategoryEntity category = CategoryEntity.builder().id(1L).name("Skincare").slug("skincare").build();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-10-11T08:00:00Z");
        Instant t2 = Instant.parse("2026-10-11T07:00:00Z");

        ProductEntity p1 = ProductEntity.builder().id(id1).category(category).sku("SKU-1").slug("sku-1").name("P1")
                .retailPrice(BigDecimal.TEN).baseWholesalePrice(BigDecimal.ONE).build();
        p1.setCreatedAt(t1);

        ProductEntity p2 = ProductEntity.builder().id(id2).category(category).sku("SKU-2").slug("sku-2").name("P2")
                .retailPrice(BigDecimal.TEN).baseWholesalePrice(BigDecimal.ONE).build();
        p2.setCreatedAt(t2);

        // Request limit=1; repository returns 2 items (limit + 1) indicating hasNext = true
        when(productRepository.findActiveProductsKeyset(any(), any(), any(), any(), any()))
                .thenReturn(List.of(p1, p2));

        BranchInventoryRepository.ProductStockTotalProjection stockProj = new BranchInventoryRepository.ProductStockTotalProjection() {
            @Override
            public UUID getProductId() {
                return id1;
            }

            @Override
            public Integer getTotalAvailable() {
                return 145;
            }
        };
        when(inventoryRepository.sumAvailableStockByProductIds(List.of(id1))).thenReturn(List.of(stockProj));

        CursorPageDto<B2bProductResponseDto> page = catalogService.listB2bProductsKeyset(1L, null, null, null, 1);

        assertThat(page.hasNext()).isTrue();
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).totalAvailableStock()).isEqualTo(145);
        assertThat(page.nextCursorCreatedAt()).isEqualTo(t1);
        assertThat(page.nextCursorId()).isEqualTo(id1);
    }
}
