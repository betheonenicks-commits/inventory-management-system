package com.iams.inventory.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
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
import com.iams.inventory.domain.Warehouse;
import com.iams.inventory.domain.WarehouseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-INV-02/03/06/08/09/10: the core stock ledger - every quantity change goes
 * through {@link #stockIn}, {@link #stockOut}, or {@link #transfer}, each of
 * which appends an immutable {@link InventoryTransaction} row and updates the
 * corresponding {@link InventoryStockBalance} in the same transaction, so the
 * two can never drift out of sync.
 */
@Service
public class InventoryStockService {

    /** US-INV-10: the currency every reporting aggregate uses - no multi-reporting-currency support exists. */
    private static final String REPORTING_CURRENCY = "USD";
    private static final int COST_SCALE = 4;

    private final InventoryItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryStockBalanceRepository balanceRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public InventoryStockService(InventoryItemRepository itemRepository, WarehouseRepository warehouseRepository,
                                  InventoryStockBalanceRepository balanceRepository,
                                  InventoryTransactionRepository transactionRepository, CurrentUserProvider currentUserProvider) {
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /** US-INV-02/06/09/10: receive stock, recalculating weighted-average cost and registering the lot/expiry if given. */
    @Transactional
    public InventoryStockBalance stockIn(StockInCommand command) {
        if (command.quantity() == null || command.quantity().signum() <= 0) {
            throw ValidationFailedException.singleField("quantity", "Must be a positive quantity");
        }
        if (command.unitCost() == null || command.unitCost().signum() < 0) {
            throw ValidationFailedException.singleField("unitCost", "A non-negative unit cost is required");
        }
        if (command.reasonCode() == null || command.reasonCode().isBlank()) {
            throw ValidationFailedException.singleField("reasonCode", "A reason code is required");
        }
        InventoryItem item = requireItem(command.itemId());
        Warehouse warehouse = requireWarehouse(command.warehouseId());
        String subLocation = normalize(command.subLocation());
        String lotNumber = normalize(command.lotNumber());

        // AC-INV-10-X: a non-reporting currency requires a real FX rate before the purchase can be saved.
        String currencyCode = command.currencyCode() != null ? command.currencyCode() : REPORTING_CURRENCY;
        BigDecimal fxRate = command.fxRate();
        if (currencyCode.equals(REPORTING_CURRENCY)) {
            fxRate = BigDecimal.ONE;
        } else if (fxRate == null || fxRate.signum() <= 0) {
            throw ValidationFailedException.singleField("fxRate",
                    "An FX rate is required to record a purchase in a non-" + REPORTING_CURRENCY + " currency");
        }
        BigDecimal reportingUnitCost = command.unitCost().multiply(fxRate).setScale(COST_SCALE, RoundingMode.HALF_UP);

        InventoryStockBalance balance = balanceRepository
                .findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouse.getId(), subLocation, lotNumber)
                .orElseGet(() -> newBalance(item, warehouse, subLocation, lotNumber));
        if (command.expiryDate() != null) {
            balance.setExpiryDate(command.expiryDate());
        }
        balance.setAverageUnitCost(nextAverageCost(item.getCostingMethod(), balance, command.quantity(), reportingUnitCost));
        balance.setQuantityOnHand(balance.getQuantityOnHand().add(command.quantity()));
        balance = balanceRepository.saveAndFlush(balance);

        appendTransaction(item, warehouse, subLocation, lotNumber, command.expiryDate(), InventoryTransactionType.STOCK_IN,
                command.quantity(), command.unitCost(), currencyCode, fxRate, reportingUnitCost, command.reasonCode(), null);
        return balance;
    }

    /** US-INV-02: issues stock out - never allowed to take a balance negative. */
    @Transactional
    public InventoryStockBalance stockOut(StockOutCommand command) {
        if (command.quantity() == null || command.quantity().signum() <= 0) {
            throw ValidationFailedException.singleField("quantity", "Must be a positive quantity");
        }
        if (command.reasonCode() == null || command.reasonCode().isBlank()) {
            throw ValidationFailedException.singleField("reasonCode", "A reason code is required");
        }
        InventoryItem item = requireItem(command.itemId());
        Warehouse warehouse = requireWarehouse(command.warehouseId());
        String subLocation = normalize(command.subLocation());
        String lotNumber = normalize(command.lotNumber());

        InventoryStockBalance balance = requireBalance(item.getId(), warehouse.getId(), subLocation, lotNumber);
        requireSufficientStock(balance, command.quantity());
        balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(command.quantity()));
        balance = balanceRepository.saveAndFlush(balance);

        appendTransaction(item, warehouse, subLocation, lotNumber, balance.getExpiryDate(), InventoryTransactionType.STOCK_OUT,
                command.quantity(), null, null, null, null, command.reasonCode(), null);
        return balance;
    }

    /**
     * US-INV-08: an atomic linked TRANSFER_OUT/TRANSFER_IN pair - both
     * transaction ids are pre-assigned before either is persisted specifically
     * so each can carry the other's id as linkedTransactionId without ever
     * needing a post-insert update (both entities are otherwise fully
     * immutable, including that column).
     */
    @Transactional
    public TransferResult transfer(StockTransferCommand command) {
        if (command.quantity() == null || command.quantity().signum() <= 0) {
            throw ValidationFailedException.singleField("quantity", "Must be a positive quantity");
        }
        if (command.reasonCode() == null || command.reasonCode().isBlank()) {
            throw ValidationFailedException.singleField("reasonCode", "A reason code is required");
        }
        InventoryItem item = requireItem(command.itemId());
        Warehouse fromWarehouse = requireWarehouse(command.fromWarehouseId());
        Warehouse toWarehouse = requireWarehouse(command.toWarehouseId());
        String fromSubLocation = normalize(command.fromSubLocation());
        String fromLotNumber = normalize(command.fromLotNumber());
        String toSubLocation = normalize(command.toSubLocation());
        String toLotNumber = normalize(command.toLotNumber());

        InventoryStockBalance source = requireBalance(item.getId(), fromWarehouse.getId(), fromSubLocation, fromLotNumber);
        requireSufficientStock(source, command.quantity());
        BigDecimal movedUnitCost = source.getAverageUnitCost();
        source.setQuantityOnHand(source.getQuantityOnHand().subtract(command.quantity()));
        balanceRepository.saveAndFlush(source);

        InventoryStockBalance destination = balanceRepository
                .findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), toWarehouse.getId(), toSubLocation, toLotNumber)
                .orElseGet(() -> newBalance(item, toWarehouse, toSubLocation, toLotNumber));
        destination.setAverageUnitCost(nextAverageCost(item.getCostingMethod(), destination, command.quantity(), movedUnitCost));
        destination.setQuantityOnHand(destination.getQuantityOnHand().add(command.quantity()));
        if (source.getExpiryDate() != null) {
            destination.setExpiryDate(source.getExpiryDate());
        }
        destination = balanceRepository.saveAndFlush(destination);

        UUID outId = UUID.randomUUID();
        UUID inId = UUID.randomUUID();
        appendTransactionWithId(outId, item, fromWarehouse, fromSubLocation, fromLotNumber, source.getExpiryDate(),
                InventoryTransactionType.TRANSFER_OUT, command.quantity(), movedUnitCost, null, null, null, command.reasonCode(), inId);
        appendTransactionWithId(inId, item, toWarehouse, toSubLocation, toLotNumber, destination.getExpiryDate(),
                InventoryTransactionType.TRANSFER_IN, command.quantity(), movedUnitCost, null, null, null, command.reasonCode(), outId);
        return new TransferResult(source, destination);
    }

    public record TransferResult(InventoryStockBalance source, InventoryStockBalance destination) {
    }

    @Transactional(readOnly = true)
    public List<InventoryStockBalance> balancesForItem(UUID itemId) {
        return balanceRepository.findByInventoryItemIdWithAssociations(itemId);
    }

    @Transactional(readOnly = true)
    public List<InventoryStockBalance> balancesForWarehouse(UUID warehouseId) {
        return balanceRepository.findByWarehouseIdWithAssociations(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> transactionsForItem(UUID itemId) {
        return transactionRepository.findByInventoryItemIdWithAssociationsOrderByPerformedAtDesc(itemId);
    }

    /** US-INV-04: every active item with a configured reorder level whose total quantity (summed across all warehouses) has crossed below it. */
    @Transactional(readOnly = true)
    public List<LowStockItem> lowStockItems() {
        return itemRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(i -> i.getReorderLevel() != null)
                .map(i -> new LowStockItem(i, balanceRepository.totalQuantityForItem(i.getId())))
                .filter(low -> low.totalQuantity().compareTo(low.item().getReorderLevel()) < 0)
                .toList();
    }

    /** US-INV-09: lots with stock still on hand expiring within the given lookahead window. */
    @Transactional(readOnly = true)
    public List<InventoryStockBalance> expiringLots(int withinDays) {
        if (withinDays < 0) {
            throw ValidationFailedException.singleField("withinDays", "Cannot be negative");
        }
        return balanceRepository.findExpiringLots(LocalDate.now().plusDays(withinDays));
    }

    private InventoryStockBalance requireBalance(UUID itemId, UUID warehouseId, String subLocation, String lotNumber) {
        return balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(itemId, warehouseId, subLocation, lotNumber)
                .orElseThrow(() -> new ConflictException("INSUFFICIENT_STOCK", "No stock recorded for this item at this location - nothing to move"));
    }

    private void requireSufficientStock(InventoryStockBalance balance, BigDecimal quantity) {
        if (balance.getQuantityOnHand().compareTo(quantity) < 0) {
            // AC-INV-02-X: a movement that would take quantity negative is rejected outright.
            throw new ConflictException("INSUFFICIENT_STOCK",
                    "Only " + balance.getQuantityOnHand() + " on hand - cannot move " + quantity);
        }
    }

    private InventoryStockBalance newBalance(InventoryItem item, Warehouse warehouse, String subLocation, String lotNumber) {
        InventoryStockBalance balance = new InventoryStockBalance();
        balance.setInventoryItem(item);
        balance.setWarehouse(warehouse);
        balance.setSubLocation(subLocation);
        balance.setLotNumber(lotNumber);
        balance.setQuantityOnHand(BigDecimal.ZERO);
        return balance;
    }

    /** US-INV-06: WEIGHTED_AVERAGE recalculates from the existing balance + this receipt; LAST_COST just takes the new price. */
    private BigDecimal nextAverageCost(CostingMethod method, InventoryStockBalance balance, BigDecimal incomingQuantity, BigDecimal incomingUnitCost) {
        if (method == CostingMethod.LAST_COST || balance.getAverageUnitCost() == null || balance.getQuantityOnHand().signum() == 0) {
            return incomingUnitCost;
        }
        BigDecimal existingValue = balance.getQuantityOnHand().multiply(balance.getAverageUnitCost());
        BigDecimal incomingValue = incomingQuantity.multiply(incomingUnitCost);
        BigDecimal totalQuantity = balance.getQuantityOnHand().add(incomingQuantity);
        return existingValue.add(incomingValue).divide(totalQuantity, COST_SCALE, RoundingMode.HALF_UP);
    }

    private void appendTransaction(InventoryItem item, Warehouse warehouse, String subLocation, String lotNumber, LocalDate expiryDate,
                                    InventoryTransactionType type, BigDecimal quantity, BigDecimal unitCost, String currencyCode,
                                    BigDecimal fxRate, BigDecimal reportingUnitCost, String reasonCode, UUID linkedTransactionId) {
        appendTransactionWithId(null, item, warehouse, subLocation, lotNumber, expiryDate, type, quantity, unitCost, currencyCode,
                fxRate, reportingUnitCost, reasonCode, linkedTransactionId);
    }

    private void appendTransactionWithId(UUID id, InventoryItem item, Warehouse warehouse, String subLocation, String lotNumber,
                                          LocalDate expiryDate, InventoryTransactionType type, BigDecimal quantity, BigDecimal unitCost,
                                          String currencyCode, BigDecimal fxRate, BigDecimal reportingUnitCost, String reasonCode,
                                          UUID linkedTransactionId) {
        CurrentUser actor = currentUserProvider.current();
        InventoryTransaction transaction = new InventoryTransaction();
        if (id != null) {
            transaction.setId(id);
        }
        transaction.setInventoryItem(item);
        transaction.setWarehouse(warehouse);
        transaction.setSubLocation(subLocation);
        transaction.setLotNumber(lotNumber);
        transaction.setExpiryDate(expiryDate);
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity);
        transaction.setUnitCost(unitCost);
        transaction.setCurrencyCode(currencyCode);
        transaction.setFxRate(fxRate);
        transaction.setReportingUnitCost(reportingUnitCost);
        transaction.setReasonCode(reasonCode);
        transaction.setPerformedByUserId(actor.id());
        transaction.setPerformedByUsername(actor.username());
        transaction.setLinkedTransactionId(linkedTransactionId);
        transactionRepository.save(transaction);
    }

    private InventoryItem requireItem(UUID id) {
        return itemRepository.findById(id).orElseThrow(() -> NotFoundException.of("InventoryItem", id));
    }

    private Warehouse requireWarehouse(UUID id) {
        return warehouseRepository.findById(id).orElseThrow(() -> NotFoundException.of("Warehouse", id));
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : "";
    }

    public record LowStockItem(InventoryItem item, BigDecimal totalQuantity) {
    }
}
