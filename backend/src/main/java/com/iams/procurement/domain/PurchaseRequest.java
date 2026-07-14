package com.iams.procurement.domain;

import com.iams.common.domain.BaseEntity;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-LIF-01: a procurement request that must be approved before a PO can be
 * created against it (US-LIF-02). Reuses {@link LifecycleRequestStatus}
 * (PENDING/APPROVED/REJECTED) rather than a duplicate enum - identical
 * semantics to Transfer/Disposal's request workflow, just a different
 * resource type.
 */
@Getter
@Setter
@Entity
@Table(name = "purchase_request")
public class PurchaseRequest extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String itemDescription;

    @Column(nullable = false, length = 1000)
    private String justification;

    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    @Column(name = "vendor_name")
    private String vendorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifecycleRequestStatus status = LifecycleRequestStatus.PENDING;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "nominal_approver_id", nullable = false)
    private UUID nominalApproverId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "effective_approver_id")
    private UUID effectiveApproverId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
