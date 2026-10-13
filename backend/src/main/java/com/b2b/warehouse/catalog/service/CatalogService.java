package com.b2b.warehouse.catalog.service;

import com.b2b.warehouse.catalog.dto.B2bProductResponseDto;
import com.b2b.warehouse.catalog.dto.CategoryDto;
import com.b2b.warehouse.catalog.dto.PriceTierDto;
import com.b2b.warehouse.catalog.dto.PublicProductResponseDto;
import com.b2b.warehouse.catalog.dto.UpsertCategoryRequestDto;
import com.b2b.warehouse.catalog.dto.UpsertProductRequestDto;
import com.b2b.warehouse.catalog.dto.WholesalePriceQuoteDto;
import com.b2b.warehouse.catalog.entity.CategoryEntity;
import com.b2b.warehouse.catalog.entity.ProductEntity;
import com.b2b.warehouse.catalog.entity.ProductPriceTierEntity;
import com.b2b.warehouse.catalog.repository.CategoryRepository;
import com.b2b.warehouse.catalog.repository.ProductRepository;
import com.b2b.warehouse.common.dto.CursorPageDto;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.inventory.repository.BranchInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final DynamicPriceTierValidator priceTierValidator;

    // =========================================================================
    // Category Management
    // =========================================================================

    @Transactional(readOnly = true)
    public List<CategoryDto> listActiveCategories() {
        return categoryRepository.findAllByIsActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listAllCategoriesForAdmin() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    @Transactional
    public CategoryDto createCategory(UpsertCategoryRequestDto request) {
        String normalizedSlug = request.slug().trim().toLowerCase();
        if (categoryRepository.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Category slug already exists: " + normalizedSlug);
        }

        CategoryEntity parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Parent category not found"));
        }

        CategoryEntity entity = CategoryEntity.builder()
                .parent(parent)
                .slug(normalizedSlug)
                .name(request.name().trim())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .isActive(request.isActive() == null || request.isActive())
                .build();

        return toCategoryDto(categoryRepository.save(entity));
    }

    // =========================================================================
    // Product & Dynamic Price Tier Management (Admin)
    // =========================================================================

    @Transactional
    public B2bProductResponseDto createProduct(UpsertProductRequestDto request) {
        String normalizedSku = request.sku().trim().toUpperCase();
        String normalizedSlug = request.slug().trim().toLowerCase();

        if (productRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Product SKU already exists: " + normalizedSku);
        }
        if (productRepository.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Product slug already exists: " + normalizedSlug);
        }

        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found"));

        List<ProductPriceTierEntity> validatedTiers = priceTierValidator.validateAndBuildEntities(
                request.priceTiers(),
                request.retailPrice()
        );

        ProductEntity product = ProductEntity.builder()
                .category(category)
                .sku(normalizedSku)
                .slug(normalizedSlug)
                .name(request.name().trim())
                .subtitle(request.subtitle())
                .description(request.description())
                .unitLabel(request.unitLabel().trim())
                .unitsPerBox(request.unitsPerBox())
                .retailPrice(request.retailPrice())
                .baseWholesalePrice(request.baseWholesalePrice())
                .sampleEnabled(request.sampleEnabled() == null || request.sampleEnabled())
                .sampleMaxQty(request.sampleMaxQty() == null ? 2 : request.sampleMaxQty())
                .weightGrams(request.weightGrams() == null ? 500 : request.weightGrams())
                .thumbnailUrl(request.thumbnailUrl())
                .galleryUrls(request.galleryUrls() == null ? List.of() : request.galleryUrls())
                .specsJson(request.specsJson() == null ? Map.of() : request.specsJson())
                .isActive(request.isActive() == null || request.isActive())
                .build();

        product.replacePriceTiers(validatedTiers);
        ProductEntity saved = productRepository.save(product);
        return toB2bProductDto(saved, 0);
    }

    @Transactional
    public B2bProductResponseDto updateProduct(UUID productId, UpsertProductRequestDto request) {
        ProductEntity product = productRepository.findWithDetailsByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));

        String normalizedSku = request.sku().trim().toUpperCase();
        String normalizedSlug = request.slug().trim().toLowerCase();

        if (productRepository.existsBySkuIgnoreCaseAndIdNot(normalizedSku, productId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Product SKU already exists: " + normalizedSku);
        }
        if (productRepository.existsBySlugIgnoreCaseAndIdNot(normalizedSlug, productId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Product slug already exists: " + normalizedSlug);
        }

        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found"));

        List<ProductPriceTierEntity> validatedTiers = priceTierValidator.validateAndBuildEntities(
                request.priceTiers(),
                request.retailPrice()
        );

        product.setCategory(category);
        product.setSku(normalizedSku);
        product.setSlug(normalizedSlug);
        product.setName(request.name().trim());
        product.setSubtitle(request.subtitle());
        product.setDescription(request.description());
        product.setUnitLabel(request.unitLabel().trim());
        product.setUnitsPerBox(request.unitsPerBox());
        product.setRetailPrice(request.retailPrice());
        product.setBaseWholesalePrice(request.baseWholesalePrice());
        if (request.sampleEnabled() != null) {
            product.setSampleEnabled(request.sampleEnabled());
        }
        if (request.sampleMaxQty() != null) {
            product.setSampleMaxQty(request.sampleMaxQty());
        }
        if (request.weightGrams() != null) {
            product.setWeightGrams(request.weightGrams());
        }
        product.setThumbnailUrl(request.thumbnailUrl());
        if (request.galleryUrls() != null) {
            product.setGalleryUrls(request.galleryUrls());
        }
        if (request.specsJson() != null) {
            product.setSpecsJson(request.specsJson());
        }
        if (request.isActive() != null) {
            product.setIsActive(request.isActive());
        }

        product.replacePriceTiers(validatedTiers);
        ProductEntity saved = productRepository.save(product);
        int totalStock = getSingleProductTotalStock(saved.getId());
        return toB2bProductDto(saved, totalStock);
    }

    /**
     * Dedicated endpoint allowing Admin to dynamically reconfigure the quantity price tiers
     * ({@code minQty} ~ {@code maxQty} and {@code unitPrice}) of a product without resending all product fields.
     */
    @Transactional
    public B2bProductResponseDto replaceProductPriceTiers(UUID productId, List<PriceTierDto> priceTiers) {
        ProductEntity product = productRepository.findWithDetailsByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));

        List<ProductPriceTierEntity> validatedTiers = priceTierValidator.validateAndBuildEntities(
                priceTiers,
                product.getRetailPrice()
        );

        product.replacePriceTiers(validatedTiers);
        ProductEntity saved = productRepository.save(product);
        int totalStock = getSingleProductTotalStock(saved.getId());
        return toB2bProductDto(saved, totalStock);
    }

    @Transactional
    public void softDeleteProduct(UUID productId) {
        ProductEntity product = productRepository.findWithDetailsByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));
        product.setIsActive(false);
        product.setDeletedAt(Instant.now());
        productRepository.save(product);
    }

    // =========================================================================
    // Public Storefront & B2B Portal Catalog Queries (Keyset Pagination + Zero N+1)
    // =========================================================================

    @Transactional(readOnly = true)
    public CursorPageDto<PublicProductResponseDto> listPublicProductsKeyset(
            Long categoryId,
            String keyword,
            Instant cursorCreatedAt,
            UUID cursorId,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<ProductEntity> fetched;
        if (cursorCreatedAt != null && cursorId != null) {
            fetched = productRepository.findActiveProductsKeyset(
                    categoryId,
                    keyword,
                    cursorCreatedAt,
                    cursorId,
                    PageRequest.of(0, safeLimit + 1)
            );
        } else {
            fetched = productRepository.findActiveProductsInitial(
                    categoryId,
                    keyword,
                    PageRequest.of(0, safeLimit + 1)
            );
        }

        boolean hasNext = fetched.size() > safeLimit;
        List<ProductEntity> pageItems = hasNext ? fetched.subList(0, safeLimit) : fetched;
        if (pageItems.isEmpty()) {
            return CursorPageDto.empty();
        }

        ProductEntity last = pageItems.get(pageItems.size() - 1);
        List<PublicProductResponseDto> dtos = pageItems.stream()
                .map(this::toPublicProductDto)
                .toList();

        return new CursorPageDto<>(
                dtos,
                hasNext ? last.getCreatedAt() : null,
                hasNext ? last.getId() : null,
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public CursorPageDto<B2bProductResponseDto> listB2bProductsKeyset(
            Long categoryId,
            String keyword,
            Instant cursorCreatedAt,
            UUID cursorId,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<ProductEntity> fetched;
        if (cursorCreatedAt != null && cursorId != null) {
            fetched = productRepository.findActiveProductsKeyset(
                    categoryId,
                    keyword,
                    cursorCreatedAt,
                    cursorId,
                    PageRequest.of(0, safeLimit + 1)
            );
        } else {
            fetched = productRepository.findActiveProductsInitial(
                    categoryId,
                    keyword,
                    PageRequest.of(0, safeLimit + 1)
            );
        }

        boolean hasNext = fetched.size() > safeLimit;
        List<ProductEntity> pageItems = hasNext ? fetched.subList(0, safeLimit) : fetched;
        if (pageItems.isEmpty()) {
            return CursorPageDto.empty();
        }

        // Batch-fetch total available stock for all products on the page in a single SQL query (Zero N+1)
        List<UUID> productIds = pageItems.stream().map(ProductEntity::getId).toList();
        Map<UUID, Integer> stockMap = inventoryRepository.sumAvailableStockByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        BranchInventoryRepository.ProductStockTotalProjection::getProductId,
                        BranchInventoryRepository.ProductStockTotalProjection::getTotalAvailable
                ));

        ProductEntity last = pageItems.get(pageItems.size() - 1);
        List<B2bProductResponseDto> dtos = pageItems.stream()
                .map(p -> toB2bProductDto(p, stockMap.getOrDefault(p.getId(), 0)))
                .toList();

        return new CursorPageDto<>(
                dtos,
                hasNext ? last.getCreatedAt() : null,
                hasNext ? last.getId() : null,
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public WholesalePriceQuoteDto calculateWholesaleQuote(UUID productId, int quantity) {
        ProductEntity product = productRepository.findWithDetailsByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));
        return priceTierValidator.calculateQuote(product, quantity);
    }

    private int getSingleProductTotalStock(UUID productId) {
        if (productId == null) {
            return 0;
        }
        return inventoryRepository.sumAvailableStockByProductIds(List.of(productId)).stream()
                .findFirst()
                .map(BranchInventoryRepository.ProductStockTotalProjection::getTotalAvailable)
                .orElse(0);
    }

    private CategoryDto toCategoryDto(CategoryEntity entity) {
        return CategoryDto.builder()
                .id(entity.getId())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .parentName(entity.getParent() != null ? entity.getParent().getName() : null)
                .slug(entity.getSlug())
                .name(entity.getName())
                .sortOrder(entity.getSortOrder() == null ? 0 : entity.getSortOrder())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .build();
    }

    private PublicProductResponseDto toPublicProductDto(ProductEntity p) {
        return PublicProductResponseDto.builder()
                .id(p.getId())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .sku(p.getSku())
                .slug(p.getSlug())
                .name(p.getName())
                .subtitle(p.getSubtitle())
                .description(p.getDescription())
                .unitLabel(p.getUnitLabel())
                .unitsPerBox(p.getUnitsPerBox() == null ? 1 : p.getUnitsPerBox())
                .retailPrice(p.getRetailPrice())
                .sampleEnabled(Boolean.TRUE.equals(p.getSampleEnabled()))
                .thumbnailUrl(p.getThumbnailUrl())
                .galleryUrls(p.getGalleryUrls())
                .specsJson(p.getSpecsJson())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private B2bProductResponseDto toB2bProductDto(ProductEntity p, int totalAvailableStock) {
        List<PriceTierDto> tierDtos = p.getPriceTiers() == null ? List.of() : p.getPriceTiers().stream()
                .sorted(Comparator.comparingInt(ProductPriceTierEntity::getMinQty))
                .map(t -> PriceTierDto.builder()
                        .id(t.getId())
                        .tierCode(t.getTierCode())
                        .rangeLabel(t.formatRangeLabel())
                        .minQty(t.getMinQty())
                        .maxQty(t.getMaxQty())
                        .unitPrice(t.getUnitPrice())
                        .discountPercent(t.getDiscountPercent())
                        .build())
                .toList();

        return B2bProductResponseDto.builder()
                .id(p.getId())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .sku(p.getSku())
                .slug(p.getSlug())
                .name(p.getName())
                .subtitle(p.getSubtitle())
                .description(p.getDescription())
                .unitLabel(p.getUnitLabel())
                .unitsPerBox(p.getUnitsPerBox() == null ? 1 : p.getUnitsPerBox())
                .retailPrice(p.getRetailPrice())
                .baseWholesalePrice(p.getBaseWholesalePrice())
                .sampleEnabled(Boolean.TRUE.equals(p.getSampleEnabled()))
                .sampleMaxQty(p.getSampleMaxQty() == null ? 2 : p.getSampleMaxQty())
                .weightGrams(p.getWeightGrams() == null ? 500 : p.getWeightGrams())
                .thumbnailUrl(p.getThumbnailUrl())
                .galleryUrls(p.getGalleryUrls())
                .specsJson(p.getSpecsJson())
                .isActive(Boolean.TRUE.equals(p.getIsActive()))
                .totalAvailableStock(totalAvailableStock)
                .priceTiers(tierDtos)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
