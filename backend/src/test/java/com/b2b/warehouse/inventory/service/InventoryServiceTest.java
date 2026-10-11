package com.b2b.warehouse.inventory.service;

import com.b2b.warehouse.branch.repository.BranchRepository;
import com.b2b.warehouse.catalog.repository.ProductRepository;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.inventory.dto.StockReservationItemDto;
import com.b2b.warehouse.inventory.repository.BranchInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private BranchInventoryRepository inventoryRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private ProductRepository productRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, branchRepository, productRepository);
    }

    @Test
    @DisplayName("Should sort items by productId ASC before atomic reservation to prevent PostgreSQL deadlocks")
    void shouldSortItemsByProductIdAscToPreventDeadlocks() {
        UUID branchId = UUID.randomUUID();
        UUID smallerProductId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID largerProductId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        // Passed in reverse order (larger UUID first, smaller UUID second)
        List<StockReservationItemDto> unsortedItems = List.of(
                StockReservationItemDto.builder().productId(largerProductId).quantity(5).build(),
                StockReservationItemDto.builder().productId(smallerProductId).quantity(10).build()
        );

        when(inventoryRepository.reserveStockAtomically(branchId, smallerProductId, 10)).thenReturn(1);
        when(inventoryRepository.reserveStockAtomically(branchId, largerProductId, 5)).thenReturn(1);

        inventoryService.reserveStockForOrder(branchId, unsortedItems);

        // Verify smallerProductId lock was acquired BEFORE largerProductId
        InOrder inOrder = inOrder(inventoryRepository);
        inOrder.verify(inventoryRepository).reserveStockAtomically(branchId, smallerProductId, 10);
        inOrder.verify(inventoryRepository).reserveStockAtomically(branchId, largerProductId, 5);
    }

    @Test
    @DisplayName("Should throw INSUFFICIENT_INVENTORY (409 Conflict) when atomic SQL update returns 0 rows")
    void shouldThrowInsufficientInventoryWhenAtomicReserveReturnsZero() {
        UUID branchId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(inventoryRepository.reserveStockAtomically(branchId, productId, 50)).thenReturn(0);

        List<StockReservationItemDto> items = List.of(
                StockReservationItemDto.builder().productId(productId).quantity(50).build()
        );

        assertThatThrownBy(() -> inventoryService.reserveStockForOrder(branchId, items))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_INVENTORY);
    }
}
