package com.iams.inventory.domain;

import com.iams.common.domain.BaseEntity;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-INV-05: a manual quantity correction (e.g. after a physical recount)
 * changes {@link InventoryStockBalance} only once approved - reuses
 * {@link LifecycleRequestStatus} rather than a duplicate enum, the same
 * reasoning {@code PurchaseRequest}'s own Javadoc gives: this is the identical
 * PENDING/APPROVED/REJECTED shape, just a different resource type. Routing
 * (nominal/effective approver) reuses {@code ApprovalRoutingService} exactly
 * as {@code DisposalService}/{@code TransferService}/{@code PurchaseRequestService} already do.
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_manual_adjustment_request")
public class InventoryManualAdjustmentRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false, updatable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @Column(name = "sub_location", nullable = false, updatable = false)
    private String subLocation = "";

    @Column(name = "lot_number", nullable = false, updatable = false)
    private String lotNumber = "";

    /** Signed - negative shrinks the balance (e.g. a recount found fewer units), positive grows it. */
    @Column(name = "quantity_delta", nullable = false, updatable = false, precision = 14, scale = 3)
    private BigDecimal quantityDelta;

    @Column(nullable = false, updatable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifecycleRequestStatus status = LifecycleRequestStatus.PENDING;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "nominal_approver_id", nullable = false, updatable = false)
    private UUID nominalApproverId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "effective_approver_id")
    private UUID effectiveApproverId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Set only once approved - the InventoryTransaction(ADJUSTMENT) row this request's approval produced. */
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "resulting_transaction_id")
    private UUID resultingTransactionId;
}
