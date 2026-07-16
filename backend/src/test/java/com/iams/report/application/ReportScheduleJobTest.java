package com.iams.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.notification.application.NotificationDispatchService;
import com.iams.notification.application.RecipientResolverService;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.infrastructure.EmailSender;
import com.iams.report.domain.ReportSchedule;
import com.iams.report.domain.ReportScheduleRepository;
import com.iams.usr.application.UserQueryService;
import com.iams.usr.application.UserWithRoles;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ReportScheduleJobTest {

    @Mock private ReportScheduleRepository repository;
    @Mock private ReportDispatchService dispatchService;
    @Mock private EmailSender emailSender;
    @Mock private UserQueryService userQueryService;
    @Mock private NotificationDispatchService notificationDispatch;
    @Mock private RecipientResolverService recipientResolver;

    private ReportScheduleJob job;
    private ReportSchedule schedule;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        job = new ReportScheduleJob(repository, dispatchService, new CsvExporter(), new XlsxExporter(),
                new PdfExporter(), emailSender, userQueryService, notificationDispatch, recipientResolver);
        ownerId = UUID.randomUUID();
        schedule = new ReportSchedule();
        schedule.setOwnerUserId(ownerId);
        schedule.setReportKey("asset-register");
        schedule.setParams("{}");
        schedule.setExportFormat("csv");
        schedule.setFrequency(ReportSchedule.Frequency.MONTHLY);
        schedule.setRecipients("a@x.org,b@x.org,c@x.org");
        schedule.setNextRunAt(Instant.now().minusSeconds(60));
        when(repository.findByStatusAndNextRunAtLessThanEqual(eq(ReportSchedule.Status.ACTIVE), any()))
                .thenReturn(List.of(schedule));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserWithRoles owner(UserStatus status) {
        AppUser user = new AppUser();
        user.setUsername("im");
        user.setStatus(status);
        Role role = new Role();
        role.setCode("INVENTORY_MANAGER");
        role.setPermissions(List.of("reports:read"));
        return new UserWithRoles(user, List.of(role));
    }

    @Test
    void runDue_deliversToAllRecipientsAndAdvancesNextRun() {
        when(userQueryService.get(ownerId)).thenReturn(owner(UserStatus.ACTIVE));
        when(dispatchService.generate(eq("asset-register"), any())).thenReturn(new TabularReport("asset-register",
                "Asset Register", Instant.now(), List.of("A"), List.of(List.of("1"))));
        Instant before = schedule.getNextRunAt();

        int ran = job.runDue();

        assertThat(ran).isEqualTo(1);
        // The AC's "delivers automatically to all 3": one message, all three addresses.
        verify(emailSender).sendWithAttachment(eq(new String[] {"a@x.org", "b@x.org", "c@x.org"}), any(), any(),
                any(), any(byte[].class), eq("text/csv"));
        assertThat(schedule.getLastOutcome()).contains("3 recipient(s)");
        assertThat(schedule.getNextRunAt()).isAfter(before);
        // Impersonation is cleaned up after the run.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void runDue_pausesAndFlagsWhenOwnerDeactivated() {
        when(userQueryService.get(ownerId)).thenReturn(owner(UserStatus.DEACTIVATED));
        when(recipientResolver.admins()).thenReturn(Set.of(UUID.randomUUID()));

        job.runDue();

        assertThat(schedule.getStatus()).isEqualTo(ReportSchedule.Status.PAUSED_OWNER_DEACTIVATED);
        assertThat(schedule.getLastOutcome()).contains("paused for reassignment");
        verify(emailSender, never()).sendWithAttachment(any(), any(), any(), any(), any(), any());
        // The AC's "flags for reassignment": an admin alert, not silence.
        verify(notificationDispatch).dispatch(eq(NotificationEventType.SECURITY_ALERT), any(), any(), any());
    }

    @Test
    void runDue_recordsFailureAndStillAdvancesInsteadOfHammering() {
        when(userQueryService.get(ownerId)).thenReturn(owner(UserStatus.ACTIVE));
        when(dispatchService.generate(any(), any())).thenThrow(new IllegalStateException("db gone"));
        Instant before = schedule.getNextRunAt();

        job.runDue();

        assertThat(schedule.getLastOutcome()).startsWith("Failed:");
        assertThat(schedule.getNextRunAt()).isAfter(before);
        assertThat(schedule.getStatus()).isEqualTo(ReportSchedule.Status.ACTIVE);
    }
}
