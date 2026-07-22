package com.iams.migration.api;

import com.iams.migration.api.dto.ImportCommitRequest;
import com.iams.migration.api.dto.ImportRunResponse;
import com.iams.migration.api.dto.ImportRunSummaryResponse;
import com.iams.migration.application.BulkImportService;
import com.iams.migration.application.ImportTemplateService;
import com.iams.migration.domain.ImportEntityType;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * EPIC-MIG bulk import (US-MIG-01/03/04). Two distinct permissions, matching the
 * story ACs: {@code imports:write} RUNS an import (template, dry-run, commit, and
 * reading back one's own run); {@code imports:read} browses the full history
 * (Super Admin / Admin / IT Security Officer) - an Inventory Manager who can run
 * imports is deliberately refused the history list (AC-MIG-04-X).
 */
@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final ImportTemplateService templateService;
    private final BulkImportService importService;

    public ImportController(ImportTemplateService templateService, BulkImportService importService) {
        this.templateService = templateService;
        this.importService = importService;
    }

    /** US-MIG-01: download the versioned import template for an entity type. */
    @GetMapping("/templates/{entityType}")
    @PreAuthorize("@perm.has('imports:write')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable ImportEntityType entityType) {
        ImportTemplateService.Template template = templateService.generate(entityType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + template.filename() + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(template.content());
    }

    /** US-MIG-03: dry-run validate an uploaded file, returning the per-row error report. Nothing is written. */
    @PostMapping("/dry-run/{entityType}")
    @PreAuthorize("@perm.has('imports:write')")
    public ImportRunResponse dryRun(@PathVariable ImportEntityType entityType,
                                    @RequestParam("file") MultipartFile file) throws IOException {
        return ImportRunResponse.from(importService.dryRun(entityType, file.getOriginalFilename(), file.getBytes()));
    }

    /** US-MIG-03: commit the rows that passed dry-run, idempotently, returning the reconciliation. */
    @PostMapping("/{runId}/commit")
    @PreAuthorize("@perm.has('imports:write')")
    public ImportRunResponse commit(@PathVariable UUID runId, @Valid @RequestBody ImportCommitRequest request) {
        return ImportRunResponse.from(importService.commit(runId, request.idempotencyKey()));
    }

    /**
     * US-MIG-03 AC-X: a run's status/reconciliation stay retrievable after a browser
     * close. Readable by the runner (imports:write) or a history viewer (imports:read).
     */
    @GetMapping("/{runId}")
    @PreAuthorize("@perm.has('imports:write') or @perm.has('imports:read')")
    public ImportRunResponse getRun(@PathVariable UUID runId) {
        return ImportRunResponse.from(importService.getRun(runId));
    }

    /** US-MIG-04: import run history - visible to Super Admin/Admin/IT Security Officer, not the Inventory Manager who runs imports. */
    @GetMapping
    @PreAuthorize("@perm.has('imports:read')")
    public List<ImportRunSummaryResponse> history() {
        return importService.history().stream().map(ImportRunSummaryResponse::from).toList();
    }
}
