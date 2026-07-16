package com.iams.report.domain;

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

/** US-RPT-13: one standing "run this report and email it" instruction. */
@Getter
@Setter
@Entity
@Table(name = "report_schedule")
public class ReportSchedule {

    public enum Frequency { DAILY, WEEKLY, MONTHLY }

    public enum Status { ACTIVE, PAUSED_OWNER_DEACTIVATED }

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "report_key", nullable = false, updatable = false, length = 40)
    private String reportKey;

    /** JSON object of the same string params GET /reports/{key} takes. */
    @Column(nullable = false, updatable = false)
    private String params;

    @Column(name = "export_format", nullable = false, updatable = false, length = 6)
    private String exportFormat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private Frequency frequency;

    /** Comma-separated email addresses, validated at creation. */
    @Column(nullable = false)
    private String recipients;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.ACTIVE;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_outcome", length = 500)
    private String lastOutcome;

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
