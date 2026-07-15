package com.iams.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findBySku(String sku);

    List<InventoryItem> findAllByOrderByNameAsc();

    List<InventoryItem> findByActiveTrueOrderByNameAsc();
}
