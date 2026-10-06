package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.dto.request.InventoryAdjustmentRequest;
import com.agri.ecommerce.dto.request.InventoryImportRequest;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock ProductRepository products;
    @Mock InventoryBatchRepository batches;
    @Mock InventoryTransactionRepository transactions;
    @Mock OrderInventoryAllocationRepository allocations;
    @Mock SupplierRepository suppliers;
    @Mock UserRepository users;
    InventoryService service;
    Product product;
    List<OrderInventoryAllocation> savedAllocations;

    @BeforeEach void setup() {
        service = new InventoryService(products, batches, transactions, allocations, suppliers, users, 3);
        product = new Product(); ReflectionTestUtils.setField(product, "id", 1L);
        product.setName("Cà chua"); product.setUnit("kg"); product.setStock(0); product.setStatus(ProductStatus.OUT_OF_STOCK);
        savedAllocations = new ArrayList<>();
        lenient().when(products.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(product));
        lenient().when(batches.findByProductIdForUpdate(1L)).thenReturn(new ArrayList<>());
        lenient().when(allocations.save(any())).thenAnswer(call -> { OrderInventoryAllocation allocation = call.getArgument(0); savedAllocations.add(allocation); return allocation; });
        lenient().when(allocations.findAllByOrderItem_Order_IdOrderByIdAsc(10L)).thenAnswer(call -> savedAllocations);
    }

    @Test void importingHundredCreatesBatchAndMovementAndIncreasesAvailableStock() {
        when(users.getReferenceById(3L)).thenReturn(new User());
        when(batches.findByProductIdForUpdate(1L)).thenAnswer(call -> List.of(capturedImport));
        when(batches.save(any())).thenAnswer(call -> { capturedImport = call.getArgument(0); ReflectionTestUtils.setField(capturedImport, "id", 7L); return capturedImport; });

        var result = service.importBatch(new InventoryImportRequest(1L, 100, BigDecimal.valueOf(12000), null, LocalDate.now().plusDays(10), null, "Thu hoạch mới"), 3L);

        assertThat(result.quantity()).isEqualTo(100);
        assertThat(result.availableQuantity()).isEqualTo(100);
        assertThat(product.getStock()).isEqualTo(100);
        verify(transactions).save(argThat(value -> value.getType().equals("IMPORT") && value.getQuantity() == 100));
    }
    private InventoryBatch capturedImport;

    @Test void reservationSplitsAcrossTwoBatchesInFefoOrderAndExportIsIdempotent() {
        InventoryBatch first = batch(11L, 10, LocalDate.now().plusDays(5));
        InventoryBatch second = batch(12L, 20, LocalDate.now().plusDays(10));
        when(batches.findByProductIdForUpdate(1L)).thenAnswer(call -> List.of(first, second));
        Order order = order(15);

        service.reserveOrder(order, Map.of(1L, product));
        assertThat(savedAllocations).extracting(OrderInventoryAllocation::getQuantity).containsExactly(10, 5);
        assertThat(first.getReservedQuantity()).isEqualTo(10);
        assertThat(second.getReservedQuantity()).isEqualTo(5);
        assertThat(product.getStock()).isEqualTo(15);

        service.exportOrder(order);
        service.exportOrder(order);
        assertThat(first.getRemainingQuantity()).isZero();
        assertThat(second.getRemainingQuantity()).isEqualTo(15);
        assertThat(savedAllocations).extracting(OrderInventoryAllocation::getStatus).containsOnly("EXPORTED");
        verify(transactions, times(2)).save(argThat(value -> value.getType().equals("EXPORT")));
    }

    @Test void expiredBatchIsSkippedAndInsufficientFreshStockIsRejected() {
        InventoryBatch expired = batch(11L, 100, LocalDate.now().minusDays(1));
        InventoryBatch fresh = batch(12L, 4, LocalDate.now().plusDays(7));
        when(batches.findByProductIdForUpdate(1L)).thenAnswer(call -> List.of(expired, fresh));
        assertThatThrownBy(() -> service.reserveOrder(order(5), Map.of(1L, product)))
            .extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        assertThat(expired.getReservedQuantity()).isZero();
    }

    @Test void batchExpiringAfterReservationIsReallocatedBeforeExport() {
        InventoryBatch first = batch(11L, 10, LocalDate.now().plusDays(1));
        InventoryBatch second = batch(12L, 20, LocalDate.now().plusDays(10));
        when(batches.findByProductIdForUpdate(1L)).thenAnswer(call -> List.of(first, second));
        Order order = order(5);
        service.reserveOrder(order, Map.of(1L, product));
        first.setExpiryDate(LocalDate.now().minusDays(1));

        service.exportOrder(order);

        assertThat(first.getRemainingQuantity()).isEqualTo(10);
        assertThat(first.getReservedQuantity()).isZero();
        assertThat(second.getRemainingQuantity()).isEqualTo(15);
        assertThat(savedAllocations).extracting(OrderInventoryAllocation::getStatus).containsExactly("RELEASED", "EXPORTED");
    }

    @Test void expiryWithinThreeDaysIsReportedAndNotSoldAfterExpiry() {
        InventoryBatch soon = batch(11L, 6, LocalDate.now().plusDays(3));
        Category category = new Category(); category.setName("Rau"); product.setCategory(category);
        when(batches.findAllByOrderByProduct_IdAscIdAsc()).thenReturn(List.of(soon));
        when(products.findAll()).thenReturn(List.of(product));
        assertThat(service.summary().expiringBatches()).isEqualTo(1);
    }

    @Test void cancellationRestoresTheExactExportedBatchesOnce() {
        InventoryBatch first = batch(11L, 10, LocalDate.now().plusDays(5));
        InventoryBatch second = batch(12L, 20, LocalDate.now().plusDays(10));
        when(batches.findByProductIdForUpdate(1L)).thenAnswer(call -> List.of(first, second));
        Order order = order(15);
        service.reserveOrder(order, Map.of(1L, product)); service.exportOrder(order);
        service.restoreOrder(order); service.restoreOrder(order);
        assertThat(first.getRemainingQuantity()).isEqualTo(10);
        assertThat(second.getRemainingQuantity()).isEqualTo(20);
        assertThat(product.getStock()).isEqualTo(30);
        assertThat(savedAllocations).extracting(OrderInventoryAllocation::getStatus).containsOnly("RESTORED");
        verify(transactions, times(2)).save(argThat(value -> value.getType().equals("ORDER_CANCEL_RESTORE")));
    }

    @Test void damagedAdjustmentReducesStockAndRecordsReason() {
        InventoryBatch batch = batch(11L, 20, LocalDate.now().plusDays(7));
        when(batches.findById(11L)).thenReturn(java.util.Optional.of(batch));
        when(batches.findByProductIdForUpdate(1L)).thenReturn(List.of(batch));
        when(users.getReferenceById(3L)).thenReturn(new User());
        service.adjust(new InventoryAdjustmentRequest(11L, -4, "DAMAGED", "Dập trong vận chuyển"), 3L);
        assertThat(batch.getRemainingQuantity()).isEqualTo(16);
        assertThat(product.getStock()).isEqualTo(16);
        verify(transactions).save(argThat(value -> value.getType().equals("DAMAGED") && value.getQuantity() == -4 && value.getReason().contains("Dập")));
    }

    @Test void cannotRemoveReservedStockDuringAdjustment() {
        InventoryBatch batch = batch(11L, 10, LocalDate.now().plusDays(7)); batch.setReservedQuantity(8);
        when(batches.findById(11L)).thenReturn(java.util.Optional.of(batch));
        when(batches.findByProductIdForUpdate(1L)).thenReturn(List.of(batch));
        assertThatThrownBy(() -> service.adjust(new InventoryAdjustmentRequest(11L, -3, "DAMAGED", "Hỏng"), 3L))
            .extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        assertThat(batch.getRemainingQuantity()).isEqualTo(10);
    }

    @Test void expiredImportIsRejectedBeforeWriting() {
        assertThatThrownBy(() -> service.importBatch(new InventoryImportRequest(1L, 100, BigDecimal.ZERO, null, LocalDate.now().minusDays(1), null, null), 3L))
            .extracting("code").isEqualTo("BATCH_EXPIRED");
        verifyNoInteractions(transactions);
    }

    private InventoryBatch batch(Long id, int amount, LocalDate expiry) {
        InventoryBatch batch = new InventoryBatch(); ReflectionTestUtils.setField(batch, "id", id);
        batch.setProduct(product); batch.setBatchCode("LOT-" + id); batch.setQuantity(amount);
        batch.setRemainingQuantity(amount); batch.setReservedQuantity(0); batch.setExpiryDate(expiry);
        batch.setImportPrice(BigDecimal.ZERO); return batch;
    }
    private Order order(int amount) {
        Order order = new Order(); ReflectionTestUtils.setField(order, "id", 10L);
        OrderItem item = new OrderItem(); ReflectionTestUtils.setField(item, "id", 20L);
        item.setProduct(product); item.setQuantity(amount); item.setPrice(BigDecimal.TEN); order.addItem(item);
        return order;
    }
}
