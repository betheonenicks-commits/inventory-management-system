package com.iams.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-ANL-01: one server-side feature-usage event. Append-only (no BaseEntity,
 * no version/updated columns - immutability is structural, like
 * AssetHistoryEvent). One row per role the actor held at action time, so
 * "module, action, and role" stays a flat, exactly-aggregatable shape even
 * for multi-role users - a user with two roles legitimately counts toward
 * both roles' adoption.
 */
@Getter
@Setter
@Entity
@Table(name = "usage_event")
public class UsageEvent {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 40)
    private String module;

    @Column(nullable = false, updatable = false, length = 60)
    private String action;

    @Column(nullable = false, updatable = false, length = 40)
    private String role;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
