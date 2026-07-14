package com.iams.org.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * FR-ORG-01: a node in the configurable multi-level hierarchy (e.g.
 * Campus->Building->Floor->Room). `parent` is null only for a root node;
 * `level` names which renameable depth (OrgLevel) this node sits at.
 * `path` is a materialized ancestor-id chain including self (see V19),
 * computed once at creation - nodes are not re-parented in this phase, so
 * it never needs recomputing. `roomVariant` (US-ORG-06) is only meaningful
 * when `level` is the Room-rank level, and must be one of that level's
 * `roomVariants`.
 */
@Getter
@Setter
@Entity
@Table(name = "org_node")
public class OrgNode {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private OrgNode parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private OrgLevel level;

    @Column(nullable = false)
    private String path;

    @Column(name = "room_variant")
    private String roomVariant;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // OrgNode predates BaseEntity (it started as a placeholder before EPIC-ORG's
    // hierarchy existed) and was never migrated onto it, so it needs its own
    // PrePersist/PreUpdate rather than inheriting BaseEntity's - without this,
    // create() paths that forget to set createdAt manually violate the DB's
    // NOT NULL constraint (found via live click-testing, 2026-07-13).
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
