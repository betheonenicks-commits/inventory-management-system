package com.iams.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-NTF-05: one user's email on/off for one event type. Absence of a row
 * means the default (email on). In-app has no preference by design - it is
 * the always-available record (US-NTF-03), and locked event types
 * (NotificationEventType.locked()) refuse edits in the service.
 */
@Getter
@Setter
@Entity
@Table(name = "notification_preference")
public class NotificationPreference {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 40)
    private NotificationEventType eventType;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
