package com.iams.migration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.migration.domain.ImportEntityType;
import com.iams.migration.domain.ImportRun;
import com.iams.migration.domain.ImportRunStatus;
import com.iams.migration.domain.ImportRunRepository;
import com.iams.sec.application.SecurityEventLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkImportServiceTest {

    @Mock private CsvParser csvParser;
    @Mock private EntityImportProcessor processor;
    @Mock private ImportRunRepository importRunRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private SecurityEventLogger securityEventLogger;

    private BulkImportService service;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(processor.entityType()).thenReturn(ImportEntityType.ASSET);
        org.mockito.Mockito.lenient().when(processor.requiredColumns()).thenReturn(List.of("name", "categoryCode"));
        service = new BulkImportService(csvParser, List.of(processor), importRunRepository, currentUserProvider, securityEventLogger);
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "im1", Set.of("INVENTORY_MANAGER")));
        org.mockito.Mockito.lenient().when(importRunRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void dryRun_collectsErrorsAndValidRows() {
        when(csvParser.parse(any())).thenReturn(List.of(
                List.of("name", "categoryCode"),
                List.of("good1", "IT-1"),
                List.of("bad", "IT-1"),
                List.of("good2", "IT-1")));
        // validate throws only for the row named "bad".
        org.mockito.Mockito.doAnswer(inv -> {
            Map<String, String> row = inv.getArgument(0);
            if ("bad".equals(row.get("name"))) {
                throw ValidationFailedException.singleField("purchaseCost", "Expected a number");
            }
            return null;
        }).when(processor).validate(any());

        ImportRun run = service.dryRun(ImportEntityType.ASSET, "assets.csv", new byte[]{1});

        assertThat(run.getStatus()).isEqualTo(ImportRunStatus.VALIDATED);
        assertThat(run.getTotalRows()).isEqualTo(3);
        assertThat(run.getValidRows()).isEqualTo(2);
        assertThat(run.getInvalidRows()).isEqualTo(1);
        assertThat(run.getErrorReport()).hasSize(1);
        assertThat(run.getErrorReport().get(0).rowNumber()).isEqualTo(2);
        assertThat(run.getErrorReport().get(0).field()).isEqualTo("purchaseCost");
        assertThat(run.getValidPayload()).hasSize(2);
    }

    @Test
    void dryRun_skipsFullyBlankRows() {
        when(csvParser.parse(any())).thenReturn(List.of(
                List.of("name", "categoryCode"),
                List.of("good", "IT-1"),
                List.of("", ""))); // trailing blank line
        ImportRun run = service.dryRun(ImportEntityType.ASSET, "f.csv", new byte[]{1});
        assertThat(run.getTotalRows()).isEqualTo(1);
        assertThat(run.getValidRows()).isEqualTo(1);
    }

    @Test
    void dryRun_rejectsMissingRequiredColumn() {
        when(csvParser.parse(any())).thenReturn(List.of(List.of("name", "manufacturer")));
        assertThatThrownBy(() -> service.dryRun(ImportEntityType.ASSET, "f.csv", new byte[]{1}))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("categoryCode");
    }

    @Test
    void dryRun_refusesNonAssetEntityType() {
        assertThatThrownBy(() -> service.dryRun(ImportEntityType.PERSON, "f.csv", new byte[]{1}))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("not available yet");
    }

    @Test
    void commit_createsValidRows_andReportsReconciliation() {
        ImportRun run = validatedRun(List.of(Map.of("name", "a"), Map.of("name", "b")), 1);
        when(importRunRepository.findById(run.getId())).thenReturn(Optional.of(run));
        when(importRunRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());

        ImportRun result = service.commit(run.getId(), "key-1");

        assertThat(result.getStatus()).isEqualTo(ImportRunStatus.COMMITTED);
        assertThat(result.getCommittedRows()).isEqualTo(2);
        assertThat(result.getFailedRows()).isEqualTo(0);
        assertThat(result.getSkippedRows()).isEqualTo(1);
        assertThat(result.getOutcome()).contains("2 created", "0 failed", "1 skipped");
        verify(processor, org.mockito.Mockito.times(2)).create(any());
    }

    @Test
    void commit_countsPerRowFailuresWithoutAborting() {
        ImportRun run = validatedRun(List.of(Map.of("name", "ok"), Map.of("name", "fail")), 0);
        when(importRunRepository.findById(run.getId())).thenReturn(Optional.of(run));
        when(importRunRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        org.mockito.Mockito.doAnswer(inv -> {
            Map<String, String> row = inv.getArgument(0);
            if ("fail".equals(row.get("name"))) {
                throw new IllegalStateException("category vanished");
            }
            return null;
        }).when(processor).create(any());

        ImportRun result = service.commit(run.getId(), "key-2");

        assertThat(result.getCommittedRows()).isEqualTo(1);
        assertThat(result.getFailedRows()).isEqualTo(1);
    }

    @Test
    void commit_isIdempotent_replaysSameKeyWithoutRecreating() {
        ImportRun run = validatedRun(List.of(Map.of("name", "a")), 0);
        run.setStatus(ImportRunStatus.COMMITTED);
        run.setIdempotencyKey("key-3");
        run.setCommittedRows(1);
        when(importRunRepository.findById(run.getId())).thenReturn(Optional.of(run));

        ImportRun result = service.commit(run.getId(), "key-3");

        assertThat(result.getCommittedRows()).isEqualTo(1);
        verify(processor, never()).create(any());
        verify(importRunRepository, never()).save(any());
    }

    @Test
    void commit_rejectsDifferentKeyOnAlreadyCommittedRun() {
        ImportRun run = validatedRun(List.of(Map.of("name", "a")), 0);
        run.setStatus(ImportRunStatus.COMMITTED);
        run.setIdempotencyKey("key-orig");
        when(importRunRepository.findById(run.getId())).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> service.commit(run.getId(), "key-different"))
                .isInstanceOf(ConflictException.class);
        verify(processor, never()).create(any());
    }

    @Test
    void commit_rejectsKeyAlreadyUsedByAnotherRun() {
        ImportRun run = validatedRun(List.of(Map.of("name", "a")), 0);
        when(importRunRepository.findById(run.getId())).thenReturn(Optional.of(run));
        when(importRunRepository.findByIdempotencyKey("shared-key")).thenReturn(Optional.of(new ImportRun()));

        assertThatThrownBy(() -> service.commit(run.getId(), "shared-key"))
                .isInstanceOf(ConflictException.class);
        verify(processor, never()).create(any());
    }

    @Test
    void commit_requiresAnIdempotencyKey() {
        assertThatThrownBy(() -> service.commit(UUID.randomUUID(), "  "))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("idempotency key");
    }

    private ImportRun validatedRun(List<Map<String, String>> validPayload, int invalidRows) {
        ImportRun run = new ImportRun();
        run.setId(UUID.randomUUID());
        run.setEntityType(ImportEntityType.ASSET);
        run.setStatus(ImportRunStatus.VALIDATED);
        run.setValidPayload(new ArrayList<>(validPayload));
        run.setInvalidRows(invalidRows);
        run.setValidRows(validPayload.size());
        run.setTotalRows(validPayload.size() + invalidRows);
        return run;
    }
}
