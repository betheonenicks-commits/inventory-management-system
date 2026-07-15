package com.iams.inventory.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryStockBalanceRepository extends JpaRepository<InventoryStockBalance, UUID> {

    Optional<InventoryStockBalance> findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(
            UUID inventoryItemId, UUID warehouseId, String subLocation, String lotNumber);

    @Query("SELECT b FROM InventoryStockBalance b JOIN FETCH b.inventoryItem JOIN FETCH b.warehouse WHERE b.inventoryItem.id = :itemId")
    List<InventoryStockBalance> findByInventoryItemIdWithAssociations(UUID itemId);

    @Query("SELECT b FROM InventoryStockBalance b JOIN FETCH b.inventoryItem JOIN FETCH b.warehouse WHERE b.warehouse.id = :warehouseId")
    List<InventoryStockBalance> findByWarehouseIdWithAssociations(UUID warehouseId);

    /** US-INV-04: aggregated across every warehouse - the reorder level is on the item, not per-location. */
    @Query("SELECT COALESCE(SUM(b.quantityOnHand), 0) FROM InventoryStockBalance b WHERE b.inventoryItem.id = :itemId")
    BigDecimal totalQuantityForItem(UUID itemId);

    boolean existsByWarehouseIdAndQuantityOnHandGreaterThan(UUID warehouseId, BigDecimal threshold);

    /** US-INV-09: real lots (lotNumber not '') with an expiry on or before the lookahead cutoff and stock still on hand. */
    @Query("SELECT b FROM InventoryStockBalance b JOIN FETCH b.inventoryItem JOIN FETCH b.warehouse "
            + "WHERE b.lotNumber <> '' AND b.expiryDate IS NOT NULL AND b.expiryDate <= :cutoff AND b.quantityOnHand > 0 "
            + "ORDER BY b.expiryDate ASC")
    List<InventoryStockBalance> findExpiringLots(LocalDate cutoff);
}
