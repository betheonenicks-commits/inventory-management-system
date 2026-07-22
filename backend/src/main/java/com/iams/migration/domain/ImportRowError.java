package com.iams.migration.domain;

/**
 * One per-row validation failure from a dry run (US-MIG-03's "per-row error
 * report"). {@code rowNumber} is 1-based over data rows (the header is row 0),
 * matching what a user sees in their spreadsheet. Serialized into the
 * {@code import_run.error_report} jsonb column, so it must stay a plain,
 * Jackson-round-trippable record.
 */
public record ImportRowError(int rowNumber, String field, String message) {
}
