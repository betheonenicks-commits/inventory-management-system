package com.iams.org.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Minimal placeholder for the future ORG module (FR-ORG-01: configurable
 * multi-level hierarchy). Asset.orgNodeId FKs here in Phase 1; only what's
 * needed to satisfy that FK is modeled - no api/application layers, since
 * nothing external calls this module yet.
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
