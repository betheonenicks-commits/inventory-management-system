package com.iams.usr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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
 * One row per (user, role) pair - the flat multi-role model (FR-USR-07).
 * Carries who assigned the role and when, cheaply, for the same reason the
 * FRS asks delegations (FR-LIF-15) to be logged: "which role got you this
 * access, and who granted it" should never require reconstructing history
 * from a general activity log that doesn't exist yet (EPIC-SEC).
 */
@Getter
@Setter
@Entity
@Table(name = "user_role_assignment")
public class UserRoleAssignment {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
}
