package com.iams.storage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.storage.domain.AttachmentRepository;
import com.iams.storage.infrastructure.ObjectStorageClient;
import com.iams.storage.infrastructure.StorageUnavailableException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachmentJanitorTest {

    @Mock private AttachmentRepository repository;
    @Mock private ObjectStorageClient storage;

    private AttachmentJanitor janitor;

    @BeforeEach
    void setUp() {
        janitor = new AttachmentJanitor(repository, storage, new StorageProperties());
    }

    @Test
    void reapsOnlyObjectsWithoutMetadataRows() {
        Instant cutoff = Instant.now();
        when(storage.listKeysOlderThan(cutoff)).thenReturn(List.of("audit_finding/orphan", "audit_finding/linked"));
        when(repository.existsByStorageKey("audit_finding/orphan")).thenReturn(false);
        when(repository.existsByStorageKey("audit_finding/linked")).thenReturn(true);

        int reaped = janitor.reapOrphans(cutoff);

        assertThat(reaped).isEqualTo(1);
        verify(storage).delete("audit_finding/orphan");
        verify(storage, never()).delete("audit_finding/linked");
    }

    @Test
    void sweepSurvivesAnUnavailableObjectStore() {
        when(storage.listKeysOlderThan(any())).thenThrow(new StorageUnavailableException("down", null));

        // The scheduler thread must not crash-loop; the next sweep simply retries.
        assertThatCode(() -> janitor.sweep()).doesNotThrowAnyException();
    }
}
