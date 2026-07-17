package com.iams.analytics.domain;

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
 * US-ANL-04: one submitted feedback item, kept as the durable record of
 * receipt (the routed notification carries the content to the configured
 * recipient; this row is what proves it was received even if that
 * notification is later dismissed). Append-only.
 */
@Getter
@Setter
@Entity
@Table(name = "feedback_item")
public class FeedbackItem {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private FeedbackCategory category;

    /** Nullable by design: category alone is accepted (US-ANL-04 AC). */
    @Column(updatable = false)
    private String message;

    /** The in-app route the user was on when submitting - context, not tracking. */
    @Column(name = "page_path", updatable = false)
    private String pagePath;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "submitted_by", nullable = false, updatable = false)
    private UUID submittedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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
