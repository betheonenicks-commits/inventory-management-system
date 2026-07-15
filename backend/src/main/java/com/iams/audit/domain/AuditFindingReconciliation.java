package com.iams.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * US-AUD-21: a Missing finding, found later outside any active audit,
 * reconciled as its own immutable row linked back to the original
 * {@link AuditFinding} - never an edit to that finding (AC-AUD-21-X). One row
 * per finding (DB-unique on finding_id): a finding can only be reconciled
 * once, the same "act on demonstrated state, refuse a repeat" idempotency
 * every other terminal action in this codebase (double-approve, double-restore,
 * double-cancel) already enforces.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_finding_reconciliation")
public class AuditFindingReconciliation {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false, updatable = false)
    private AuditFinding finding;

    @Column(name = "found_location_note", nullable = false, updatable = false, length = 500)
    private String foundLocationNote;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "reconciled_by_user_id", nullable = false, updatable = false)
    private UUID reconciledByUserId;

    @Column(name = "reconciled_by_username", nullable = false, updatable = false)
    private String reconciledByUsername;

    @Column(name = "reconciled_at", nullable = false, updatable = false)
    private Instant reconciledAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (reconciledAt == null) {
            reconciledAt = Instant.now();
        }
    }
}
