package com.iams.lifecycle.domain;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.common.domain.BaseEntity;
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
 * US-LIF-09/12: retire/dispose/donate an asset with a mandatory reason and
 * approval, restorable within a configurable window. disposalHistoryEvent
 * links to the AssetHistoryEvent written at approval time, so
 * RestoreService can record the restore as a correction of it
 * (AssetHistoryEvent.correctionOfEvent) - the original disposal event is
 * never edited, only linked-from (AC-LIF-12-H).
 */
@Getter
@Setter
@Entity
@Table(name = "asset_disposal_request")
public class AssetDisposalRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposal_type", nullable = false)
    private DisposalType disposalType;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_history_event_id")
    private AssetHistoryEvent disposalHistoryEvent;

    @Column(name = "restored_at")
    private Instant restoredAt;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "restored_by")
    private UUID restoredBy;
}
