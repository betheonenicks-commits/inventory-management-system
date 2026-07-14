package com.iams.compliance.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-CMP-06: overrides normal retention/anonymization timing for a specific
 * asset or audit until explicitly lifted. Lift is gated compliance:write at
 * the controller (AC-CMP-06-X: "refused" for anyone who isn't a Compliance
 * Officer or Super Admin) - compliance:write is only ever granted to
 * COMPLIANCE_OFFICER and SUPER_ADMIN's wildcard (V15), so the existing
 * permission model already expresses this exactly, no extra role check needed.
 */
@Getter
@Setter
@Entity
@Table(name = "legal_hold")
public class LegalHold extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, updatable = false)
    private LegalHoldScopeType scopeType;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "scope_id", nullable = false, updatable = false)
    private UUID scopeId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "lifted_by")
    private UUID liftedBy;

    @Column(name = "lifted_at")
    private Instant liftedAt;

    @Column(name = "lift_reason", length = 500)
    private String liftReason;
}
