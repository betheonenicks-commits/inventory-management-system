package com.iams.inventory.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryItemCostingMethodChangeRepository extends JpaRepository<InventoryItemCostingMethodChange, UUID> {

    @Query("SELECT c FROM InventoryItemCostingMethodChange c WHERE c.inventoryItem.id = :itemId ORDER BY c.changedAt ASC")
    List<InventoryItemCostingMethodChange> findByInventoryItemIdOrderByChangedAtAsc(UUID itemId);
}
