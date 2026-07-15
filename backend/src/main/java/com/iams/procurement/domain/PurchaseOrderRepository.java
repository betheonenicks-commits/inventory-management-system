package com.iams.procurement.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    @Query("SELECT po FROM PurchaseOrder po JOIN FETCH po.purchaseRequest WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdWithRequest(UUID id);

    @Query("SELECT po FROM PurchaseOrder po JOIN FETCH po.purchaseRequest ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findAllWithRequestOrderByCreatedAtDesc();

    @Query("SELECT po FROM PurchaseOrder po JOIN FETCH po.purchaseRequest WHERE po.status = :status ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findByStatusWithRequestOrderByCreatedAtDesc(PurchaseOrderStatus status);

    /** US-INV-07: a vendor's full purchase history. */
    @Query("SELECT po FROM PurchaseOrder po JOIN FETCH po.purchaseRequest WHERE po.vendor.id = :vendorId ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findByVendorIdWithRequestOrderByCreatedAtDesc(UUID vendorId);
}
