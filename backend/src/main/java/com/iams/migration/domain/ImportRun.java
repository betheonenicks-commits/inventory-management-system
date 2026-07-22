package com.iams.migration.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One bulk-import run (US-MIG-03/04). Created by a dry run (holding the per-row
 * error report and the validated rows), then mutated in place by commit (holding
 * the reconciliation counts and idempotency key). {@code createdBy}/{@code createdAt}
 * (from {@link BaseEntity}) are the submitter and submitted-at the history view shows.
 */
@Getter
@Setter
@Entity
@Table(name = "import_run")
public class ImportRun extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private ImportEntityType entityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportRunStatus status;

    @Column(name = "template_version")
    private String templateVersion;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows;

    @Column(name = "committed_rows")
    private Integer committedRows;

    @Column(name = "failed_rows")
    private Integer failedRows;

    @Column(name = "skipped_rows")
    private Integer skippedRows;

    @Column
    private String outcome;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_report", nullable = false)
    private List<ImportRowError> errorReport = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valid_payload", nullable = false)
    private List<Map<String, String>> validPayload = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "committed_by")
    private UUID committedBy;

    @Column(name = "committed_at")
    private Instant committedAt;
}
