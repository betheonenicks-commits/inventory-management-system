package com.iams.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.report.domain.ReportSchedule;
import com.iams.report.domain.ReportScheduleRepository;
import java.time.Instant;
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
class ReportScheduleServiceTest {

    @Mock private ReportScheduleRepository repository;
    @Mock private ReportDispatchService dispatchService;
    @Mock private CurrentUserProvider currentUserProvider;

    private ReportScheduleService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new ReportScheduleService(repository, dispatchService, currentUserProvider);
        userId = UUID.randomUUID();
        lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(userId, "im", Set.of("INVENTORY_MANAGER"), Set.of("reports:read")));
        lenient().when(repository.save(any(ReportSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_storesOwnerParamsAndFutureFirstRun() {
        ReportSchedule schedule = service.create("asset-register", Map.of("categoryId", "x"), "xlsx",
                ReportSchedule.Frequency.MONTHLY, List.of("a@x.org", " b@x.org "));

        assertThat(schedule.getOwnerUserId()).isEqualTo(userId);
        assertThat(schedule.getRecipients()).isEqualTo("a@x.org,b@x.org");
        assertThat(schedule.getNextRunAt()).isAfter(Instant.now().plusSeconds(29 * 24 * 3600));
        assertThat(ReportScheduleService.readParams(schedule.getParams())).containsEntry("categoryId", "x");
    }

    @Test
    void create_rejectsBadFormatBadEmailAndEmptyRecipients() {
        assertThatThrownBy(() -> service.create("asset-register", Map.of(), "docx",
                ReportSchedule.Frequency.DAILY, List.of("a@x.org")))
                .isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.create("asset-register", Map.of(), "csv",
                ReportSchedule.Frequency.DAILY, List.of("not-an-email")))
                .isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.create("asset-register", Map.of(), "csv",
                ReportSchedule.Frequency.DAILY, List.of()))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void delete_isNotFoundForAnotherUsersSchedule() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndOwnerUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void next_advancesByFrequency() {
        Instant from = Instant.parse("2026-07-16T00:00:00Z");
        assertThat(ReportScheduleService.next(from, ReportSchedule.Frequency.DAILY))
                .isEqualTo(Instant.parse("2026-07-17T00:00:00Z"));
        assertThat(ReportScheduleService.next(from, ReportSchedule.Frequency.WEEKLY))
                .isEqualTo(Instant.parse("2026-07-23T00:00:00Z"));
    }
}
