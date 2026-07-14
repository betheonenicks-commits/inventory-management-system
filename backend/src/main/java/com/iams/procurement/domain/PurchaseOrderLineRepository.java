package com.iams.procurement.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, UUID> {

    @Query("SELECT l FROM PurchaseOrderLine l JOIN FETCH l.purchaseOrder WHERE l.id = :id")
    Optional<PurchaseOrderLine> findByIdWithOrder(UUID id);

    @Query("SELECT l FROM PurchaseOrderLine l WHERE l.purchaseOrder.id = :purchaseOrderId ORDER BY l.createdAt ASC")
    List<PurchaseOrderLine> findByPurchaseOrderIdOrderByCreatedAtAsc(UUID purchaseOrderId);
}
