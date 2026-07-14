package com.iams.lifecycle.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-LIF-15: a Department Head (or anyone routed approvals) delegating their
 * approval authority to a named alternate for a defined window - e.g. going
 * on leave. Plain UUID references, not JPA relations to AppUser, matching
 * this codebase's established convention for actor references (see
 * Asset.assignedToPersonId, Audit.nominalApproverId).
 */
@Getter
@Setter
@Entity
@Table(name = "approval_delegation")
public class ApprovalDelegation extends BaseEntity {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "delegator_user_id", nullable = false)
    private UUID delegatorUserId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "delegate_user_id", nullable = false)
    private UUID delegateUserId;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(nullable = false)
    private boolean active = true;

    private String reason;
}
