package com.iams.audit.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * US-AUD-02: one auditor's assignment to an audit, optionally to a named
 * sub-scope. Extends BaseEntity (unlike the append-only audit-log style
 * entities in this module) because an assignment genuinely mutates once: it
 * ends. auditorUsername is a denormalized snapshot captured at assignment
 * time, same reasoning AuditFinding.verifiedByUsername uses - assignment
 * history should read correctly even if the user is later renamed, and it
 * avoids an extra join on every assignment list read.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_assignment")
public class AuditAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false, updatable = false)
    private Audit audit;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "auditor_user_id", nullable = false, updatable = false)
    private UUID auditorUserId;

    @Column(name = "auditor_username", nullable = false, updatable = false)
    private String auditorUsername;

    /** e.g. "Building B, Floor 2" - optional split of a bulk audit's scope (US-AUD-02). */
    @Column(name = "sub_scope")
    private String subScope;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "unassigned_at")
    private Instant unassignedAt;
}
