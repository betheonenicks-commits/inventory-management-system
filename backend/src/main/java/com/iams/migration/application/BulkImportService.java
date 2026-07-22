package com.iams.migration.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationErrorItem;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.migration.domain.ImportEntityType;
import com.iams.migration.domain.ImportRowError;
import com.iams.migration.domain.ImportRun;
import com.iams.migration.domain.ImportRunRepository;
import com.iams.migration.domain.ImportRunStatus;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-MIG-03: dry-run validate a bulk import into a per-row error report, then
 * explicitly commit only the valid rows with an idempotency key and a
 * reconciliation report; US-MIG-04: the run persists as one traceable record.
 * <p>
 * Entity-agnostic: it dispatches to whichever {@link EntityImportProcessor} is
 * registered for the requested type (Spring injects every processor bean). An
 * entity type with no processor is refused with a clear message, not a silent
 * no-op.
 */
@Service
public class BulkImportService {

    private final CsvParser csvParser;
    private final Map<ImportEntityType, EntityImportProcessor> processors;
    private final ImportRunRepository importRunRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SecurityEventLogger securityEventLogger;

    public BulkImportService(CsvParser csvParser, List<EntityImportProcessor> processorBeans,
                             ImportRunRepository importRunRepository, CurrentUserProvider currentUserProvider,
                             SecurityEventLogger securityEventLogger) {
        this.csvParser = csvParser;
        this.processors = new EnumMap<>(ImportEntityType.class);
        for (EntityImportProcessor processor : processorBeans) {
            this.processors.put(processor.entityType(), processor);
        }
        this.importRunRepository = importRunRepository;
        this.currentUserProvider = currentUserProvider;
        this.securityEventLogger = securityEventLogger;
    }

    /**
     * Parse and validate every data row without writing anything, persisting the
     * per-row error report and the rows that passed so a later commit needs no
     * re-upload. Read-only against the domain, but writes the ImportRun record.
     */
    @Transactional
    public ImportRun dryRun(ImportEntityType entityType, String filename, byte[] content) {
        EntityImportProcessor processor = requireProcessor(entityType);
        List<List<String>> rows = csvParser.parse(content);
        if (rows.isEmpty() || isBlankRow(rows.get(0))) {
            throw ValidationFailedException.singleField("file", "The file is empty or has no header row");
        }
        List<String> headers = rows.get(0).stream().map(String::trim).toList();
        requireRequiredColumns(processor, headers);

        List<Map<String, String>> validPayload = new ArrayList<>();
        List<ImportRowError> errors = new ArrayList<>();
        int dataRows = 0;
        for (int i = 1; i < rows.size(); i++) {
            List<String> cells = rows.get(i);
            if (isBlankRow(cells)) {
                continue; // a trailing/blank spreadsheet line is not a data row
            }
            dataRows++;
            int rowNumber = dataRows; // 1-based over actual data rows, matching the user's mental model
            Map<String, String> row = toRowMap(headers, cells);
            try {
                processor.validate(row);
                validPayload.add(row);
            } catch (ValidationFailedException e) {
                for (ValidationErrorItem item : e.getErrors()) {
                    errors.add(new ImportRowError(rowNumber, item.field(), item.message()));
                }
            } catch (NotFoundException e) {
                errors.add(new ImportRowError(rowNumber, "row", e.getMessage()));
            }
        }

        ImportRun run = new ImportRun();
        run.setEntityType(entityType);
        run.setStatus(ImportRunStatus.VALIDATED);
        run.setTemplateVersion(processor.templateVersion());
        run.setOriginalFilename(filename);
        run.setTotalRows(dataRows);
        run.setValidRows(validPayload.size());
        run.setInvalidRows(dataRows - validPayload.size());
        run.setErrorReport(errors);
        run.setValidPayload(validPayload);
        run.setCreatedBy(currentUserProvider.current().id());
        return importRunRepository.save(run);
    }

    /**
     * Commit the rows that passed dry-run. Deliberately NOT @Transactional at this
     * level: each asset create runs in its own transaction inside
     * AssetRegistrationService, so a single row that fails at commit time (e.g. its
     * category was deleted since the dry run) is counted and skipped rather than
     * rolling back the whole batch. Idempotent (AC-MIG-03-H): replaying the same
     * key returns the cached reconciliation without creating anything again.
     */
    public ImportRun commit(UUID runId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ValidationFailedException.singleField("idempotencyKey", "An idempotency key is required to commit an import");
        }
        ImportRun run = importRunRepository.findById(runId).orElseThrow(() -> NotFoundException.of("ImportRun", runId));

        if (run.getStatus() == ImportRunStatus.COMMITTED) {
            if (idempotencyKey.equals(run.getIdempotencyKey())) {
                return run; // idempotent replay - the cached reconciliation, nothing re-created
            }
            throw new ConflictException("IMPORT_ALREADY_COMMITTED", "This import run has already been committed");
        }
        importRunRepository.findByIdempotencyKey(idempotencyKey).ifPresent(other -> {
            throw new ConflictException("IDEMPOTENCY_KEY_REUSED", "This idempotency key was already used for a different import run");
        });
        EntityImportProcessor processor = requireProcessor(run.getEntityType());

        int created = 0;
        int failed = 0;
        for (Map<String, String> row : run.getValidPayload()) {
            try {
                processor.create(row);
                created++;
            } catch (RuntimeException e) {
                failed++;
            }
        }
        int skipped = run.getInvalidRows(); // rows that never passed dry-run are skipped, never committed

        UUID actor = currentUserProvider.current().id();
        run.setIdempotencyKey(idempotencyKey);
        run.setCommittedRows(created);
        run.setFailedRows(failed);
        run.setSkippedRows(skipped);
        run.setOutcome(created + " created / " + failed + " failed / " + skipped + " skipped");
        run.setStatus(ImportRunStatus.COMMITTED);
        run.setCommittedBy(actor);
        run.setCommittedAt(Instant.now());
        run.setUpdatedBy(actor);
        // repository.save() carries its own transaction (SimpleJpaRepository is @Transactional),
        // so this persists without re-wrapping the non-transactional commit loop above.
        ImportRun saved = importRunRepository.save(run);
        securityEventLogger.record(SecurityEventType.BULK_IMPORT_COMMITTED, actor, null, null,
                "Committed import run " + runId + " (" + run.getEntityType() + "): " + run.getOutcome());
        return saved;
    }

    @Transactional(readOnly = true)
    public ImportRun getRun(UUID runId) {
        return importRunRepository.findById(runId).orElseThrow(() -> NotFoundException.of("ImportRun", runId));
    }

    @Transactional(readOnly = true)
    public List<ImportRun> history() {
        return importRunRepository.findAllByOrderByCreatedAtDesc();
    }

    private EntityImportProcessor requireProcessor(ImportEntityType entityType) {
        EntityImportProcessor processor = processors.get(entityType);
        if (processor == null) {
            throw ValidationFailedException.singleField("entityType",
                    entityType + " import is not available yet - no importer is registered for it in this release");
        }
        return processor;
    }

    private void requireRequiredColumns(EntityImportProcessor processor, List<String> headers) {
        for (String required : processor.requiredColumns()) {
            if (!headers.contains(required)) {
                throw ValidationFailedException.singleField("file",
                        "Missing required column '" + required + "'. Download the template to see the expected columns.");
            }
        }
    }

    private Map<String, String> toRowMap(List<String> headers, List<String> cells) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int c = 0; c < headers.size(); c++) {
            row.put(headers.get(c), c < cells.size() ? cells.get(c) : "");
        }
        return row;
    }

    private boolean isBlankRow(List<String> cells) {
        return cells.stream().allMatch(cell -> cell == null || cell.isBlank());
    }
}
