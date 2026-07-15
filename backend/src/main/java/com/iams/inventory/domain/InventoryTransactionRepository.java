package com.iams.inventory.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    @Query("SELECT t FROM InventoryTransaction t JOIN FETCH t.inventoryItem JOIN FETCH t.warehouse "
            + "WHERE t.inventoryItem.id = :itemId ORDER BY t.performedAt DESC")
    List<InventoryTransaction> findByInventoryItemIdWithAssociationsOrderByPerformedAtDesc(UUID itemId);

    @Query("SELECT t FROM InventoryTransaction t JOIN FETCH t.inventoryItem JOIN FETCH t.warehouse "
            + "WHERE t.warehouse.id = :warehouseId ORDER BY t.performedAt DESC")
    List<InventoryTransaction> findByWarehouseIdWithAssociationsOrderByPerformedAtDesc(UUID warehouseId);
}
