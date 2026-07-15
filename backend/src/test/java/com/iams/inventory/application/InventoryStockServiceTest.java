package com.iams.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.CostingMethod;
import com.iams.inventory.domain.InventoryItem;
import com.iams.inventory.domain.InventoryItemRepository;
import com.iams.inventory.domain.InventoryStockBalance;
import com.iams.inventory.domain.InventoryStockBalanceRepository;
import com.iams.inventory.domain.InventoryTransaction;
import com.iams.inventory.domain.InventoryTransactionRepository;
import com.iams.inventory.domain.InventoryTransactionType;
import com.iams.inventory.domain.UnitOfMeasure;
import com.iams.inventory.domain.Warehouse;
import com.iams.inventory.domain.WarehouseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryStockServiceTest {

    @Mock private InventoryItemRepository itemRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private InventoryStockBalanceRepository balanceRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private InventoryStockService service;
    private InventoryItem item;
    private Warehouse warehouseA;
    private Warehouse warehouseB;

    @BeforeEach
    void setUp() {
        service = new InventoryStockService(itemRepository, warehouseRepository, balanceRepository, transactionRepository, currentUserProvider);
        item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setUnitOfMeasure(UnitOfMeasure.BOX);
        item.setCostingMethod(CostingMethod.WEIGHTED_AVERAGE);
        warehouseA = new Warehouse();
        warehouseA.setId(UUID.randomUUID());
        warehouseB = new Warehouse();
        warehouseB.setId(UUID.randomUUID());

        org.mockito.Mockito.lenient().when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        org.mockito.Mockito.lenient().when(warehouseRepository.findById(warehouseA.getId())).thenReturn(Optional.of(warehouseA));
        org.mockito.Mockito.lenient().when(warehouseRepository.findById(warehouseB.getId())).thenReturn(Optional.of(warehouseB));
        org.mockito.Mockito.lenient().when(balanceRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "invmgr", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void stockIn_rejectsNonPositiveQuantity() {
        StockInCommand command = new StockInCommand(item.getId(), warehouseA.getId(), null, null, null,
                BigDecimal.ZERO, BigDecimal.TEN, null, null, "Initial stock");

        assertThatThrownBy(() -> service.stockIn(command)).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void stockIn_requiresFxRate_forNonReportingCurrency() {
        StockInCommand command = new StockInCommand(item.getId(), warehouseA.getId(), null, null, null,
                BigDecimal.TEN, BigDecimal.TEN, "EUR", null, "Purchase");

        assertThatThrownBy(() -> service.stockIn(command)).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void stockIn_firstReceipt_averageCostEqualsUnitCost() {
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseA.getId(), "", ""))
                .thenReturn(Optional.empty());
        StockInCommand command = new StockInCommand(item.getId(), warehouseA.getId(), null, null, null,
                new BigDecimal("10"), new BigDecimal("5.00"), null, null, "Initial stock");

        InventoryStockBalance result = service.stockIn(command);

        assertThat(result.getQuantityOnHand()).isEqualByComparingTo("10");
        assertThat(result.getAverageUnitCost()).isEqualByComparingTo("5.0000");
    }

    @Test
    void stockIn_weightedAverage_recalculatesAcrossTwoReceipts() {
        InventoryStockBalance existing = balanceOf(item, warehouseA, new BigDecimal("10"), new BigDecimal("5.00"));
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseA.getId(), "", ""))
                .thenReturn(Optional.of(existing));
        // 10 units @ 5.00 already on hand; receiving 10 more @ 7.00 -> (50 + 70) / 20 = 6.00
        StockInCommand command = new StockInCommand(item.getId(), warehouseA.getId(), null, null, null,
                new BigDecimal("10"), new BigDecimal("7.00"), null, null, "Restock");

        InventoryStockBalance result = service.stockIn(command);

        assertThat(result.getQuantityOnHand()).isEqualByComparingTo("20");
        assertThat(result.getAverageUnitCost()).isEqualByComparingTo("6.0000");
    }

    @Test
    void stockIn_lastCost_overwritesRatherThanAveraging() {
        item.setCostingMethod(CostingMethod.LAST_COST);
        InventoryStockBalance existing = balanceOf(item, warehouseA, new BigDecimal("10"), new BigDecimal("5.00"));
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseA.getId(), "", ""))
                .thenReturn(Optional.of(existing));
        StockInCommand command = new StockInCommand(item.getId(), warehouseA.getId(), null, null, null,
                new BigDecimal("10"), new BigDecimal("7.00"), null, null, "Restock");

        InventoryStockBalance result = service.stockIn(command);

        assertThat(result.getAverageUnitCost()).isEqualByComparingTo("7.0000");
    }

    @Test
    void stockIn_recordsReportingUnitCost_fromFxRate() {
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseA.getId(), "", ""))
                .thenReturn(Optional.empty());
        StockInCommand command = new StockInCommand(item.getId(), warehouseA.getId(), null, null, null,
                new BigDecimal("10"), new BigDecimal("500"), "EUR", new BigDecimal("1.08"), "Import purchase");

        service.stockIn(command);

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        org.mockito.Mockito.verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrencyCode()).isEqualTo("EUR");
        assertThat(captor.getValue().getReportingUnitCost()).isEqualByComparingTo("540.0000");
    }

    @Test
    void stockOut_rejectsWhenNoBalanceRecorded() {
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseA.getId(), "", ""))
                .thenReturn(Optional.empty());
        StockOutCommand command = new StockOutCommand(item.getId(), warehouseA.getId(), null, null, BigDecimal.ONE, "Issued");

        assertThatThrownBy(() -> service.stockOut(command)).isInstanceOf(ConflictException.class);
    }

    @Test
    void stockOut_rejectsWhenInsufficientStock_neverGoesNegative() {
        InventoryStockBalance existing = balanceOf(item, warehouseA, new BigDecimal("3"), new BigDecimal("5.00"));
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseA.getId(), "", ""))
                .thenReturn(Optional.of(existing));
        StockOutCommand command = new StockOutCommand(item.getId(), warehouseA.getId(), null, null, new BigDecimal("5"), "Issued to Dept");

        assertThatThrownBy(() -> service.stockOut(command)).isInstanceOf(ConflictException.class);
        assertThat(existing.getQuantityOnHand()).isEqualByComparingTo("3");
    }

    @Test
    void stockOut_decrementsExactly() {
        InventoryStockBalance existing = balanceOf(item, warehouseA, new BigDecimal("25"), new BigDecimal("5.00"));
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseA.getId(), "", ""))
                .thenReturn(Optional.of(existing));
        StockOutCommand command = new StockOutCommand(item.getId(), warehouseA.getId(), null, null, new BigDecimal("5"), "Issued to Dept");

        InventoryStockBalance result = service.stockOut(command);

        assertThat(result.getQuantityOnHand()).isEqualByComparingTo("20");
    }

    @Test
    void transfer_rejectsWhenSourceInsufficient() {
        InventoryStockBalance source = balanceOf(item, warehouseA, new BigDecimal("2"), new BigDecimal("5.00"));
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseA.getId(), "", ""))
                .thenReturn(Optional.of(source));
        StockTransferCommand command = new StockTransferCommand(item.getId(), warehouseA.getId(), null, null,
                warehouseB.getId(), null, null, new BigDecimal("10"), "Rebalance");

        assertThatThrownBy(() -> service.transfer(command)).isInstanceOf(ConflictException.class);
    }

    @Test
    void transfer_movesQuantityAtomically_withLinkedTransactionPair() {
        InventoryStockBalance source = balanceOf(item, warehouseA, new BigDecimal("10"), new BigDecimal("5.00"));
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseA.getId(), "", ""))
                .thenReturn(Optional.of(source));
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouseB.getId(), "", ""))
                .thenReturn(Optional.empty());
        StockTransferCommand command = new StockTransferCommand(item.getId(), warehouseA.getId(), null, null,
                warehouseB.getId(), null, null, new BigDecimal("4"), "Rebalance");

        InventoryStockService.TransferResult result = service.transfer(command);

        assertThat(result.source().getQuantityOnHand()).isEqualByComparingTo("6");
        assertThat(result.destination().getQuantityOnHand()).isEqualByComparingTo("4");
        assertThat(result.destination().getAverageUnitCost()).isEqualByComparingTo("5.0000");

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        org.mockito.Mockito.verify(transactionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<InventoryTransaction> saved = captor.getAllValues();
        InventoryTransaction out = saved.stream().filter(t -> t.getTransactionType() == InventoryTransactionType.TRANSFER_OUT).findFirst().orElseThrow();
        InventoryTransaction in = saved.stream().filter(t -> t.getTransactionType() == InventoryTransactionType.TRANSFER_IN).findFirst().orElseThrow();
        assertThat(out.getLinkedTransactionId()).isEqualTo(in.getId());
        assertThat(in.getLinkedTransactionId()).isEqualTo(out.getId());
    }

    @Test
    void lowStockItems_flagsOnlyItemsBelowReorderLevel() {
        InventoryItem lowItem = new InventoryItem();
        lowItem.setId(UUID.randomUUID());
        lowItem.setName("Nitrile Gloves");
        lowItem.setReorderLevel(new BigDecimal("20"));
        InventoryItem okItem = new InventoryItem();
        okItem.setId(UUID.randomUUID());
        okItem.setReorderLevel(new BigDecimal("20"));
        InventoryItem unconfiguredItem = new InventoryItem();
        unconfiguredItem.setId(UUID.randomUUID());
        unconfiguredItem.setReorderLevel(null);

        when(itemRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(lowItem, okItem, unconfiguredItem));
        when(balanceRepository.totalQuantityForItem(lowItem.getId())).thenReturn(new BigDecimal("18"));
        when(balanceRepository.totalQuantityForItem(okItem.getId())).thenReturn(new BigDecimal("25"));

        List<InventoryStockService.LowStockItem> result = service.lowStockItems();

        assertThat(result).extracting(low -> low.item().getId()).containsExactly(lowItem.getId());
    }

    @Test
    void expiringLots_rejectsNegativeLookahead() {
        assertThatThrownBy(() -> service.expiringLots(-1)).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void expiringLots_queriesCutoffDateFromToday() {
        service.expiringLots(30);

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        org.mockito.Mockito.verify(balanceRepository).findExpiringLots(captor.capture());
        assertThat(captor.getValue()).isEqualTo(LocalDate.now().plusDays(30));
    }

    private InventoryStockBalance balanceOf(InventoryItem item, Warehouse warehouse, BigDecimal quantity, BigDecimal averageCost) {
        InventoryStockBalance balance = new InventoryStockBalance();
        balance.setInventoryItem(item);
        balance.setWarehouse(warehouse);
        balance.setSubLocation("");
        balance.setLotNumber("");
        balance.setQuantityOnHand(quantity);
        balance.setAverageUnitCost(averageCost);
        return balance;
    }
}
