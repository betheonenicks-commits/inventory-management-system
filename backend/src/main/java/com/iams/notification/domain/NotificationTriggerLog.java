package com.iams.notification.domain;

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
 * US-NTF-06's exactly-once ledger: one row per (event, entity, threshold).
 * The DB unique constraint is the actual dedup - the sweep INSERTs first and
 * dispatches only if the insert won, so two overlapping sweeps can't
 * double-fire.
 */
@Getter
@Setter
@Entity
@Table(name = "notification_trigger_log")
public class NotificationTriggerLog {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 40)
    private NotificationEventType eventType;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "entity_id", nullable = false, updatable = false)
    private UUID entityId;

    @Column(name = "threshold_key", nullable = false, updatable = false, length = 60)
    private String thresholdKey;

    @Column(name = "fired_at", nullable = false, updatable = false)
    private Instant firedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (firedAt == null) {
            firedAt = Instant.now();
        }
    }
}
