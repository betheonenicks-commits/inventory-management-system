package com.iams.audit.domain;

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
 * US-AUD-24: a correction to a single field of a recorded finding, added as
 * its own immutable row rather than an edit to {@link AuditFinding}. Mirrors
 * AssetHistoryEvent's correctionOfEvent pattern (fieldName/oldValue/newValue)
 * exactly, applied to findings instead of asset history rows. The original
 * finding is looked up alongside its corrections list by callers - the
 * "effective" value of a corrected field is whatever the most recent
 * correction says, computed in AuditFindingMapper, never written back onto
 * the finding itself.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_finding_correction")
public class AuditFindingCorrection {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false, updatable = false)
    private AuditFinding finding;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_name", nullable = false, updatable = false)
    private CorrectionField fieldName;

    @Column(name = "old_value", updatable = false)
    private String oldValue;

    @Column(name = "new_value", nullable = false, updatable = false)
    private String newValue;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(name = "actor_username", nullable = false, updatable = false)
    private String actorUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
