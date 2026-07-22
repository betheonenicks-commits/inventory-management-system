package com.iams.migration.api.dto;

import com.iams.migration.domain.ImportEntityType;
import com.iams.migration.domain.ImportRowError;
import com.iams.migration.domain.ImportRun;
import com.iams.migration.domain.ImportRunStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full detail of an import run, including the per-row error report - returned by
 * the dry-run and single-run endpoints (US-MIG-03). The validated payload is
 * deliberately not exposed; it is an internal staging detail, not user output.
 */
public record ImportRunResponse(
        UUID id,
        ImportEntityType entityType,
        ImportRunStatus status,
        String templateVersion,
        String originalFilename,
        int totalRows,
        int validRows,
        int invalidRows,
        Integer committedRows,
        Integer failedRows,
        Integer skippedRows,
        String outcome,
        List<ImportRowError> errorReport,
        UUID submittedBy,
        Instant submittedAt,
        UUID committedBy,
        Instant committedAt
) {
    public static ImportRunResponse from(ImportRun run) {
        return new ImportRunResponse(
                run.getId(), run.getEntityType(), run.getStatus(), run.getTemplateVersion(),
                run.getOriginalFilename(), run.getTotalRows(), run.getValidRows(), run.getInvalidRows(),
                run.getCommittedRows(), run.getFailedRows(), run.getSkippedRows(), run.getOutcome(),
                run.getErrorReport(), run.getCreatedBy(), run.getCreatedAt(),
                run.getCommittedBy(), run.getCommittedAt());
    }
}
