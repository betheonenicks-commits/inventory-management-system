package com.iams.report.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.report.domain.ReportSchedule;
import com.iams.report.domain.ReportScheduleRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-RPT-13: create/list/delete standing report deliveries. Own-rows-only
 * like saved searches and export jobs; validation happens at creation so the
 * scheduled runner never meets a malformed schedule.
 */
@Service
public class ReportScheduleService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_RECIPIENTS = 10;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReportScheduleRepository repository;
    private final ReportDispatchService dispatchService;
    private final CurrentUserProvider currentUserProvider;

    public ReportScheduleService(ReportScheduleRepository repository, ReportDispatchService dispatchService,
                                 CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.dispatchService = dispatchService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ReportSchedule create(String reportKey, Map<String, String> params, String exportFormat,
                                 ReportSchedule.Frequency frequency, List<String> recipients) {
        dispatchService.requireKnownKey(reportKey);
        if (!List.of("csv", "xlsx", "pdf").contains(exportFormat == null ? "" : exportFormat.toLowerCase())) {
            throw ValidationFailedException.singleField("exportFormat", "must be one of csv, xlsx, pdf");
        }
        if (frequency == null) {
            throw ValidationFailedException.singleField("frequency", "is required");
        }
        if (recipients == null || recipients.isEmpty() || recipients.size() > MAX_RECIPIENTS) {
            throw ValidationFailedException.singleField("recipients",
                    "Between 1 and " + MAX_RECIPIENTS + " email addresses are required");
        }
        List<String> cleaned = recipients.stream().map(r -> r == null ? "" : r.trim()).toList();
        for (String recipient : cleaned) {
            if (!EMAIL.matcher(recipient).matches()) {
                throw ValidationFailedException.singleField("recipients", "'" + recipient + "' is not a valid email address");
            }
        }

        ReportSchedule schedule = new ReportSchedule();
        schedule.setOwnerUserId(currentUserProvider.current().id());
        schedule.setReportKey(reportKey);
        schedule.setParams(writeParams(params == null ? Map.of() : params));
        schedule.setExportFormat(exportFormat.toLowerCase());
        schedule.setFrequency(frequency);
        schedule.setRecipients(String.join(",", cleaned));
        schedule.setNextRunAt(firstRun(frequency));
        return repository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<ReportSchedule> listOwn() {
        return repository.findByOwnerUserIdOrderByCreatedAtDesc(currentUserProvider.current().id());
    }

    @Transactional
    public void delete(UUID id) {
        ReportSchedule schedule = repository.findByIdAndOwnerUserId(id, currentUserProvider.current().id())
                .orElseThrow(() -> NotFoundException.of("ReportSchedule", id));
        repository.delete(schedule);
    }

    /** Daily runs start tomorrow, weekly in a week, monthly in ~a month - a schedule is a standing order, not a run-now. */
    private static Instant firstRun(ReportSchedule.Frequency frequency) {
        return next(Instant.now(), frequency);
    }

    static Instant next(Instant from, ReportSchedule.Frequency frequency) {
        return switch (frequency) {
            case DAILY -> from.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> from.plus(7, ChronoUnit.DAYS);
            case MONTHLY -> from.plus(30, ChronoUnit.DAYS);
        };
    }

    static Map<String, String> readParams(String json) {
        try {
            return MAPPER.readValue(json == null || json.isBlank() ? "{}" : json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt schedule params", e);
        }
    }

    private static String writeParams(Map<String, String> params) {
        try {
            return MAPPER.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unserializable schedule params", e);
        }
    }

    static String[] recipientsOf(ReportSchedule schedule) {
        return Arrays.stream(schedule.getRecipients().split(",")).map(String::trim).toArray(String[]::new);
    }
}
