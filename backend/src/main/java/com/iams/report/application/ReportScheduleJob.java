package com.iams.report.application;

import com.iams.common.security.CurrentUser;
import com.iams.notification.application.NotificationDispatchService;
import com.iams.notification.application.RecipientResolverService;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.infrastructure.EmailSender;
import com.iams.report.domain.ReportSchedule;
import com.iams.report.domain.ReportScheduleRepository;
import com.iams.usr.application.UserQueryService;
import com.iams.usr.application.UserWithRoles;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * US-RPT-13's runner. Each due schedule is generated UNDER ITS OWNER'S
 * IDENTITY - a CurrentUser is reconstructed from the owner's live roles and
 * set on the security context for exactly the duration of the run, so a
 * scoped Department Head's scheduled report contains what their own
 * foreground run would, never more (the same scope-safety reasoning as the
 * export-job path's DelegatingSecurityContextExecutor, extended to runs that
 * outlive any request). A deactivated owner PAUSES the schedule and raises
 * an admin alert - the AC's "flags for reassignment rather than failing
 * silently forever".
 */
@Component
public class ReportScheduleJob {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduleJob.class);

    private final ReportScheduleRepository repository;
    private final ReportDispatchService dispatchService;
    private final CsvExporter csvExporter;
    private final XlsxExporter xlsxExporter;
    private final PdfExporter pdfExporter;
    private final EmailSender emailSender;
    private final UserQueryService userQueryService;
    private final NotificationDispatchService notificationDispatch;
    private final RecipientResolverService recipientResolver;

    public ReportScheduleJob(ReportScheduleRepository repository, ReportDispatchService dispatchService,
                             CsvExporter csvExporter, XlsxExporter xlsxExporter, PdfExporter pdfExporter,
                             EmailSender emailSender, UserQueryService userQueryService,
                             NotificationDispatchService notificationDispatch,
                             RecipientResolverService recipientResolver) {
        this.repository = repository;
        this.dispatchService = dispatchService;
        this.csvExporter = csvExporter;
        this.xlsxExporter = xlsxExporter;
        this.pdfExporter = pdfExporter;
        this.emailSender = emailSender;
        this.userQueryService = userQueryService;
        this.notificationDispatch = notificationDispatch;
        this.recipientResolver = recipientResolver;
    }

    @Scheduled(fixedDelayString = "${iams.reports.schedule-sweep-ms:300000}")
    public void sweep() {
        runDue();
    }

    public int runDue() {
        List<ReportSchedule> due =
                repository.findByStatusAndNextRunAtLessThanEqual(ReportSchedule.Status.ACTIVE, Instant.now());
        int ran = 0;
        for (ReportSchedule schedule : due) {
            runOne(schedule);
            ran++;
        }
        return ran;
    }

    private void runOne(ReportSchedule schedule) {
        UserWithRoles owner;
        try {
            owner = userQueryService.get(schedule.getOwnerUserId());
        } catch (Exception e) {
            pauseForOwner(schedule, "Owner account no longer exists");
            return;
        }
        if (owner.user().getStatus() != UserStatus.ACTIVE) {
            pauseForOwner(schedule, "Owner account is deactivated");
            return;
        }

        var previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            impersonate(owner);
            TabularReport report =
                    dispatchService.generate(schedule.getReportKey(), ReportScheduleService.readParams(schedule.getParams()));
            byte[] rendered = render(schedule.getExportFormat(), report);
            String fileName = report.key() + "-" + LocalDate.now() + "." + schedule.getExportFormat();
            String[] recipients = ReportScheduleService.recipientsOf(schedule);
            emailSender.sendWithAttachment(recipients,
                    "[IAMS] Scheduled report: " + report.title(),
                    "Attached: " + report.title() + ", generated " + Instant.now()
                            + " on the " + schedule.getFrequency().name().toLowerCase() + " schedule owned by "
                            + owner.user().getUsername() + ".",
                    fileName, rendered, contentTypeOf(schedule.getExportFormat()));
            schedule.setLastOutcome("Delivered to " + recipients.length + " recipient(s)");
        } catch (Exception e) {
            String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            schedule.setLastOutcome(("Failed: " + error).substring(0, Math.min(500, ("Failed: " + error).length())));
            log.warn("Scheduled report {} ({}) failed: {}", schedule.getId(), schedule.getReportKey(), error);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
        schedule.setLastRunAt(Instant.now());
        // Always advance - a failing schedule reports its outcome and tries
        // again next period instead of hammering every sweep.
        schedule.setNextRunAt(ReportScheduleService.next(Instant.now(), schedule.getFrequency()));
        repository.save(schedule);
    }

    private void pauseForOwner(ReportSchedule schedule, String why) {
        schedule.setStatus(ReportSchedule.Status.PAUSED_OWNER_DEACTIVATED);
        schedule.setLastOutcome(why + " - schedule paused for reassignment");
        repository.save(schedule);
        notificationDispatch.dispatch(NotificationEventType.SECURITY_ALERT, recipientResolver.admins(),
                Map.of("summary", "Scheduled report paused - owner unavailable",
                        "detail", "The " + schedule.getFrequency() + " '" + schedule.getReportKey()
                                + "' schedule (" + schedule.getId() + ") was paused: " + why
                                + ". Reassign or delete it."),
                "/reports");
        log.warn("Report schedule {} paused: {}", schedule.getId(), why);
    }

    private void impersonate(UserWithRoles owner) {
        var roleCodes = owner.roles().stream().map(Role::getCode).collect(Collectors.toSet());
        var permissions = owner.roles().stream().flatMap(r -> r.getPermissions().stream()).collect(Collectors.toSet());
        CurrentUser currentUser =
                new CurrentUser(owner.user().getId(), owner.user().getUsername(), roleCodes, permissions);
        var authorities = roleCodes.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, authorities));
    }

    private byte[] render(String format, TabularReport report) {
        return switch (format) {
            case "csv" -> csvExporter.export(report);
            case "pdf" -> pdfExporter.export(report);
            default -> xlsxExporter.export(report);
        };
    }

    private static String contentTypeOf(String format) {
        return switch (format) {
            case "csv" -> "text/csv";
            case "pdf" -> "application/pdf";
            default -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
    }
}
