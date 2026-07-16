package com.iams.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Runs the executor synchronously (Runnable::run) so job state transitions
 * are assertable without sleeping on real threads.
 */
@ExtendWith(MockitoExtension.class)
class ExportJobServiceTest {

    @Mock private CurrentUserProvider currentUserProvider;

    private ExportJobService service;
    private UUID userId;

    private static final TabularReport REPORT =
            new TabularReport("r", "R", Instant.now(), List.of("A"), List.of(List.of("1")));

    @BeforeEach
    void setUp() {
        service = new ExportJobService(currentUserProvider, Runnable::run);
        userId = UUID.randomUUID();
        // lenient: get_isNotFoundForUnknownJob never touches the provider (null short-circuits first)
        lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(userId, "viewer1", Set.of("VIEWER"), Set.of("reports:read")));
    }

    @Test
    void submit_completesWithRenderedContentAndFullProgress() {
        ExportJobService.ExportJob job = service.submit("r", "csv", () -> REPORT,
                r -> "rendered".getBytes(StandardCharsets.UTF_8));

        assertThat(job.getStatus()).isEqualTo(ExportJobService.Status.COMPLETED);
        assertThat(job.getProgress()).isEqualTo(100);
        assertThat(job.getFileName()).startsWith("r-").endsWith(".csv");
        assertThat(service.download(job.getId()).content()).asString(StandardCharsets.UTF_8).isEqualTo("rendered");
    }

    @Test
    void submit_marksJobFailedWithoutLeakingInternals() {
        ExportJobService.ExportJob job = service.submit("r", "csv",
                () -> { throw new IllegalStateException("secret internal detail"); }, r -> new byte[0]);

        assertThat(job.getStatus()).isEqualTo(ExportJobService.Status.FAILED);
        assertThat(job.getError()).doesNotContain("secret");
        assertThatThrownBy(() -> service.download(job.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("failed");
    }

    @Test
    void get_isNotFoundForAnotherUsersJob() {
        ExportJobService.ExportJob job = service.submit("r", "csv", () -> REPORT, r -> new byte[0]);

        when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "other", Set.of("VIEWER"), Set.of("reports:read")));
        assertThatThrownBy(() -> service.get(job.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void get_isNotFoundForUnknownJob() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }
}
