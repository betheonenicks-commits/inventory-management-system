package com.iams.procurement.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * US-LIF-02: created from (and always linked back to) an approved
 * {@link PurchaseRequest} - "PO created without an approved request behind it
 * ... is blocked" (AC-LIF-02-X) is read as an unconditional rule, the same
 * simplest-safe-reading approach TransferService's Javadoc already took for
 * "requires approval per policy" with no policy engine anywhere in this
 * codebase to make it configurable.
 */
@Getter
@Setter
@Entity
@Table(name = "purchase_order")
public class PurchaseOrder extends BaseEntity {

    @Column(name = "po_number", nullable = false, unique = true, updatable = false)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_request_id", nullable = false, updatable = false)
    private PurchaseRequest purchaseRequest;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus status = PurchaseOrderStatus.OPEN;
}
