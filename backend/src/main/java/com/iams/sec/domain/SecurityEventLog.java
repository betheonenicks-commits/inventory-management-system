package com.iams.sec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * US-SEC-04: an append-only Security & Access Log row. Deliberately does NOT
 * extend BaseEntity, same reasoning as AssetHistoryEvent - no @Version, no
 * updated_by/updated_at, no controller method ever mutates one.
 * actorUserId is a plain UUID (not a JPA relation) rather than the usual
 * @ManyToOne pattern, because it must stay nullable and populatable even
 * when the "actor" doesn't resolve to a real user (a failed login against an
 * unknown username) - forcing a relation would mean either a fake user row
 * or a LEFT JOIN everywhere this is read, for no benefit since nothing here
 * needs to navigate to the User entity, only display its id/username.
 */
@Getter
@Setter
@Entity
@Table(name = "security_event_log")
public class SecurityEventLog {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    private SecurityEventType eventType;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "username_attempted", updatable = false)
    private String usernameAttempted;

    @Column(name = "ip_address", updatable = false)
    private String ipAddress;

    @Column(updatable = false)
    private String detail;

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
