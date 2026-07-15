package com.iams.audit.domain;

import com.iams.asset.domain.Asset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-AUD-05/09/10/12/24: one asset's resolution within an audit - verified,
 * missing, out of scope, or scope-changed. Deliberately NOT BaseEntity. The
 * fields that represent "what was actually found" (status, condition,
 * remarks, verifier, timestamp, device) are marked @Column(updatable=false):
 * US-AUD-24 requires those be immutable after creation - corrected only via
 * a new linked {@link AuditFindingCorrection} row, never an in-place edit. No
 * controller method is ever registered that would PATCH or DELETE a finding
 * a real scan produced; that is what actually enforces AC-AUD-24-X, not a
 * database constraint. The one exception: AuditWorkflowService.reject()
 * deletes a system-classified Missing row (verifiedByUserId null) it created
 * during the submission being rejected - that was never a scan result, so
 * undoing it is not an edit to recorded evidence, it's retracting a
 * classification the reopened audit's own rejection invalidated. No user
 * action can trigger this against an arbitrary finding by id.
 * scopeChangeDisposition is deliberately the one mutable field:
 * it isn't a correction to a recording mistake, it's a workflow disposition
 * (US-AUD-23) resolved after the fact, gating closure rather than describing
 * what was found.
 * <p>
 * verifiedByUserId/verifiedByUsername are null for a MISSING finding created
 * by system classification at audit submission (US-AUD-09) - there is no
 * human verifier for "never scanned."
 * US-AUD-21: a MISSING finding found later, outside any active audit, is
 * never edited either - see {@link AuditFindingReconciliation}, the same
 * new-linked-record discipline as {@link AuditFindingCorrection}.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_finding")
public class AuditFinding {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false, updatable = false)
    private Audit audit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, updatable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FindingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private AssetCondition condition;

    @Column(updatable = false, length = 1000)
    private String remarks;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "verified_by_user_id", updatable = false)
    private UUID verifiedByUserId;

    @Column(name = "verified_by_username", updatable = false)
    private String verifiedByUsername;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private Instant verifiedAt;

    @Column(name = "device_id", updatable = false)
    private String deviceId;

    /** US-AUD-23: only set when status = SCOPE_CHANGED; null means the disposition is still open. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_change_disposition")
    private ScopeChangeDisposition scopeChangeDisposition;

    /**
     * US-AUD-09/21: the asset's own status immediately before a MISSING
     * finding also flipped it to the MISSING {@code AssetStatusDef} - null
     * for every other finding status. {@link AuditFindingReconciliation}
     * reverts to exactly this value rather than a fixed fallback, the same
     * "capture the real prior state" discipline RepairEvent.previousStatusCode
     * already established in EPIC-LIF.
     */
    @Column(name = "previous_status_code", updatable = false)
    private String previousStatusCode;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (verifiedAt == null) {
            verifiedAt = Instant.now();
        }
    }
}
