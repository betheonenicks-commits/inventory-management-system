package com.iams.migration.application;

import com.iams.migration.domain.ImportEntityType;
import java.util.List;
import java.util.Map;

/**
 * The per-entity-type contract the bulk-import engine ({@link BulkImportService},
 * {@link ImportTemplateService}) works against, so the engine itself is entity-
 * agnostic: adding a new importable entity is a new implementation registered as
 * a Spring bean, with no change to the engine or the controller. Each processor
 * owns the single column list its template and its validator both use (US-MIG-01's
 * "the template matches exactly what the validator checks"), and delegates the
 * actual field validation/creation to the entity's own existing service so the
 * dry run and a real create can never drift.
 */
public interface EntityImportProcessor {

    ImportEntityType entityType();

    /** Ordered template columns - also the keys the validator reads from each row. */
    List<String> columns();

    /** Columns that must be present in the uploaded header (a missing one fails the whole file). */
    List<String> requiredColumns();

    /** One illustrative data row for the template, so a user sees expected formats. */
    List<String> sampleRow();

    /** Template version, surfaced in the download filename so a superseded template is recognisable. */
    String templateVersion();

    /**
     * Validate one raw column-value row exactly as a real create would, writing
     * nothing. Throws {@link com.iams.common.exception.ValidationFailedException}
     * (or {@link com.iams.common.exception.NotFoundException}) naming the offending
     * column; the engine turns those into per-row errors.
     */
    void validate(Map<String, String> row);

    /** Create the entity from one raw row - runs in the underlying service's own transaction. */
    void create(Map<String, String> row);
}
