package com.iams.migration.api.dto;

import com.iams.migration.domain.ImportEntityType;
import com.iams.migration.domain.ImportRun;
import com.iams.migration.domain.ImportRunStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * US-MIG-04 history row: who ran each import, when, with what counts and outcome -
 * without the (potentially large) per-row error report the detail view carries.
 */
public record ImportRunSummaryResponse(
        UUID id,
        ImportEntityType entityType,
        ImportRunStatus status,
        String originalFilename,
        int totalRows,
        int validRows,
        int invalidRows,
        Integer committedRows,
        Integer failedRows,
        Integer skippedRows,
        String outcome,
        UUID submittedBy,
        Instant submittedAt,
        UUID committedBy,
        Instant committedAt
) {
    public static ImportRunSummaryResponse from(ImportRun run) {
        return new ImportRunSummaryResponse(
                run.getId(), run.getEntityType(), run.getStatus(), run.getOriginalFilename(),
                run.getTotalRows(), run.getValidRows(), run.getInvalidRows(),
                run.getCommittedRows(), run.getFailedRows(), run.getSkippedRows(), run.getOutcome(),
                run.getCreatedBy(), run.getCreatedAt(), run.getCommittedBy(), run.getCommittedAt());
    }
}
