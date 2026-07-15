package com.iams.inventory.domain;

import com.iams.lifecycle.domain.LifecycleRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryManualAdjustmentRequestRepository extends JpaRepository<InventoryManualAdjustmentRequest, UUID> {

    @Query("SELECT r FROM InventoryManualAdjustmentRequest r JOIN FETCH r.inventoryItem JOIN FETCH r.warehouse WHERE r.id = :id")
    Optional<InventoryManualAdjustmentRequest> findByIdWithAssociations(UUID id);

    @Query("SELECT r FROM InventoryManualAdjustmentRequest r JOIN FETCH r.inventoryItem JOIN FETCH r.warehouse ORDER BY r.requestedAt DESC")
    List<InventoryManualAdjustmentRequest> findAllWithAssociationsOrderByRequestedAtDesc();

    @Query("SELECT r FROM InventoryManualAdjustmentRequest r JOIN FETCH r.inventoryItem JOIN FETCH r.warehouse "
            + "WHERE r.status = :status ORDER BY r.requestedAt DESC")
    List<InventoryManualAdjustmentRequest> findByStatusWithAssociationsOrderByRequestedAtDesc(LifecycleRequestStatus status);
}
