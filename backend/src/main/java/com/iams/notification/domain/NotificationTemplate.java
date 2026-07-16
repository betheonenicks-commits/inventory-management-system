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
 * US-NTF-09: one immutable version of one event/channel template. Saving an
 * edit inserts version N+1; sends always use the latest version, and
 * already-sent notifications keep their rendered text (Notification stores
 * the output, not a template reference).
 */
@Getter
@Setter
@Entity
@Table(name = "notification_template")
public class NotificationTemplate {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 40)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private NotificationChannel channel;

    @Column(nullable = false, updatable = false)
    private int version;

    @Column(nullable = false, updatable = false, length = 200)
    private String subject;

    @Column(nullable = false, updatable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
