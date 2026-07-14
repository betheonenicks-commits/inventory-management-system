package com.iams.lifecycle.domain;

import com.iams.asset.domain.Asset;
import com.iams.common.domain.BaseEntity;
import com.iams.org.domain.OrgNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-LIF-05: an asset transfer between org nodes and/or custodians, gated
 * behind approval. "Transfer Pending" (AC-LIF-05-H) is represented by this
 * request's own PENDING status rather than a mutation of Asset.status -
 * asset status models physical/operational state (in use, under repair...),
 * this models workflow state; conflating them would need reverting
 * Asset.status on rejection for no real benefit. fromOrgNode/fromPersonId
 * are snapshots of the asset's state at request time, kept even after
 * approval - the immutable record of what a transfer actually changed.
 */
@Getter
@Setter
@Entity
@Table(name = "asset_transfer_request")
public class AssetTransferRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_org_node_id")
    private OrgNode fromOrgNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_org_node_id", nullable = false)
    private OrgNode toOrgNode;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "from_person_id")
    private UUID fromPersonId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "to_person_id")
    private UUID toPersonId;

    @Column(nullable = false, length = 500)
    private String reason;

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
