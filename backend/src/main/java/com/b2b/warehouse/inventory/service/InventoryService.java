package com.b2b.warehouse.inventory.service;

import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.branch.repository.BranchRepository;
import com.b2b.warehouse.catalog.entity.ProductEntity;
import com.b2b.warehouse.catalog.repository.ProductRepository;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.inventory.dto.BranchInventoryDto;
import com.b2b.warehouse.inventory.dto.StockReservationItemDto;
import com.b2b.warehouse.inventory.dto.UpsertBranchInventoryRequestDto;
import com.b2b.warehouse.inventory.entity.BranchInventoryEntity;
import com.b2b.warehouse.inventory.repository.BranchInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Multi-Branch Inventory Engine providing deadlock-free, race-condition-proof atomic stock operations.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BranchInventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<BranchInventoryDto> getInventoryByBranch(UUID branchId) {
        return inventoryRepository.findAllByBranch_IdOrderByProduct_NameAsc(branchId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchInventoryDto> getInventoryByProduct(UUID productId) {
        return inventoryRepository.findAllByProduct_IdOrderByBranch_NameAsc(productId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchInventoryDto> getLowStockAlerts() {
        return inventoryRepository.findLowStockAlerts().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Admin/Manager stock configuration or restock adjustment for a specific (branch, product) pair.
     */
    @Transactional
    public BranchInventoryDto upsertBranchStock(UpsertBranchInventoryRequestDto request) {
        BranchInventoryEntity entity = inventoryRepository
                .findByBranch_IdAndProduct_Id(request.branchId(), request.productId())
                .orElseGet(() -> {
                    BranchEntity branch = branchRepository.findById(request.branchId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Branch not found"));
                    ProductEntity product = productRepository.findById(request.productId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));
                    return BranchInventoryEntity.builder()
                            .branch(branch)
                            .product(product)
                            .reservedQty(0)
                            .safetyStock(10)
                            .build();
                });

        entity.setAvailableQty(request.availableQty());
        if (request.safetyStock() != null) {
            entity.setSafetyStock(request.safetyStock());
        }
        entity.setUpdatedAt(Instant.now());

        return toDto(inventoryRepository.save(entity));
    }

    /**
     * Atomically reserves stock across multiple order items at a warehouse branch.
     *
     * <p><b>Deadlock Prevention</b>: Items are sorted deterministically by {@code productId ASC}
     * before acquiring PostgreSQL row locks via {@code reserveStockAtomically}.</p>
     *
     * @throws BusinessException {@code ERR_INSUFFICIENT_INVENTORY} (409 Conflict) if any item lacks sufficient stock,
     *                           rolling back the entire transaction.
     */
    @Transactional
    public void reserveStockForOrder(UUID branchId, List<StockReservationItemDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<StockReservationItemDto> sortedItems = items.stream()
                .sorted(Comparator.comparing(StockReservationItemDto::productId))
                .toList();

        for (StockReservationItemDto item : sortedItems) {
            int updatedRows = inventoryRepository.reserveStockAtomically(
                    branchId,
                    item.productId(),
                    item.quantity()
            );
            if (updatedRows == 0) {
                throw new BusinessException(
                        ErrorCode.INSUFFICIENT_INVENTORY,
                        "Insufficient stock for productId=" + item.productId()
                                + " (requested=" + item.quantity() + ") at branchId=" + branchId
                );
            }
        }
    }

    /**
     * Atomically releases previously reserved stock back to {@code available_qty}
     * (triggered on order cancellation or 30-minute unpaid order timeout).
     */
    @Transactional
    public void releaseReservedStockForOrder(UUID branchId, List<StockReservationItemDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<StockReservationItemDto> sortedItems = items.stream()
                .sorted(Comparator.comparing(StockReservationItemDto::productId))
                .toList();

        for (StockReservationItemDto item : sortedItems) {
            int updatedRows = inventoryRepository.releaseReservedStockAtomically(
                    branchId,
                    item.productId(),
                    item.quantity()
            );
            if (updatedRows == 0) {
                throw new BusinessException(
                        ErrorCode.CONCURRENT_MODIFICATION,
                        "Failed to release reserved stock for productId=" + item.productId() + " at branchId=" + branchId
                );
            }
        }
    }

    /**
     * Permanently deducts {@code reserved_qty} when an order is shipped from the warehouse.
     */
    @Transactional
    public void confirmShipmentDeduction(UUID branchId, List<StockReservationItemDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<StockReservationItemDto> sortedItems = items.stream()
                .sorted(Comparator.comparing(StockReservationItemDto::productId))
                .toList();

        for (StockReservationItemDto item : sortedItems) {
            int updatedRows = inventoryRepository.confirmReservedStockDeductionAtomically(
                    branchId,
                    item.productId(),
                    item.quantity()
            );
            if (updatedRows == 0) {
                throw new BusinessException(
                        ErrorCode.CONCURRENT_MODIFICATION,
                        "Failed to deduct reserved stock on shipment for productId=" + item.productId()
                );
            }
        }
    }

    private BranchInventoryDto toDto(BranchInventoryEntity entity) {
        int available = entity.getAvailableQty() == null ? 0 : entity.getAvailableQty();
        int safety = entity.getSafetyStock() == null ? 0 : entity.getSafetyStock();
        return BranchInventoryDto.builder()
                .id(entity.getId())
                .branchId(entity.getBranch().getId())
                .branchCode(entity.getBranch().getCode())
                .branchName(entity.getBranch().getName())
                .productId(entity.getProduct().getId())
                .productSku(entity.getProduct().getSku())
                .productName(entity.getProduct().getName())
                .unitLabel(entity.getProduct().getUnitLabel())
                .availableQty(available)
                .reservedQty(entity.getReservedQty() == null ? 0 : entity.getReservedQty())
                .safetyStock(safety)
                .lowStockWarning(available <= safety)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
