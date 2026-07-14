package com.iams.usr.domain;

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
import com.iams.org.domain.OrgNode;

/**
 * A login account (FR-USR-01). personId is a plain UUID reference rather than
 * a JPA relation, mirroring how asset.assigned_to_person_id already links to
 * Person without a hard entity coupling: a Person doesn't require a login
 * (FR-ORG-04), and symmetrically a User isn't required to have a Person
 * record (e.g. a system-only account). orgScopeNode is nullable - an
 * unscoped role (typically SUPER_ADMIN) has no restriction to enforce.
 */
@Getter
@Setter
@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String email;

    @Column(name = "person_id")
    private UUID personId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_scope_node_id")
    private OrgNode orgScopeNode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /** US-SEC-09: resets to 0 on any successful login. */
    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    /** US-SEC-09: null unless currently locked - this being in the future IS the locked state, no separate flag. */
    @Column(name = "locked_until")
    private Instant lockedUntil;
}
