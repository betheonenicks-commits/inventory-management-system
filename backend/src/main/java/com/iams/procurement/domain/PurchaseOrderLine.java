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
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * US-LIF-03/16: one line item's ordered-vs-received-vs-returned quantities.
 * quantityReceived/quantityReturned are running totals maintained by
 * PurchaseOrderService as PurchaseOrderLineEvent rows are appended - the
 * same "mutable current-state + separate immutable event log" split this
 * codebase already uses for Asset/AssetHistoryEvent and Audit/AuditFinding.
 */
@Getter
@Setter
@Entity
@Table(name = "purchase_order_line")
public class PurchaseOrderLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false, updatable = false)
    private PurchaseOrder purchaseOrder;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "quantity_ordered", nullable = false)
    private int quantityOrdered;

    @Column(name = "quantity_received", nullable = false)
    private int quantityReceived = 0;

    @Column(name = "quantity_returned", nullable = false)
    private int quantityReturned = 0;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderLineStatus status = PurchaseOrderLineStatus.OPEN;
}
