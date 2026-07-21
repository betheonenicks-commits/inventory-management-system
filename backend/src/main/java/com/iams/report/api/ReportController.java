package com.iams.report.api;

import com.iams.analytics.application.TrackUsage;
import com.iams.common.exception.ValidationFailedException;
import com.iams.report.application.AdHocReportService;
import com.iams.report.application.AdoptionReportService;
import com.iams.report.application.CsvExporter;
import com.iams.report.domain.AdHocReport;
import com.iams.report.application.ExportJobService;
import com.iams.report.application.ExportJobService.RenderFunction;
import com.iams.report.application.PdfExporter;
import com.iams.report.application.ReportDispatchService;
import com.iams.report.application.ReportScheduleJob;
import com.iams.report.application.ReportScheduleService;
import com.iams.report.application.ReportService;
import com.iams.report.domain.ReportSchedule;
import com.iams.report.application.TabularReport;
import com.iams.report.application.XlsxExporter;
import java.util.HashMap;
import java.util.Map;
import com.iams.sec.domain.SecurityEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * EPIC-RPT (BR-10). Report endpoints ride on reports:read - seeded in V15
 * onto INVENTORY_MANAGER/DEPARTMENT_HEAD/VIEWER but, like dashboards:read
 * before EPIC-DSH, never consumed by any endpoint until now; V40 grants it
 * to the other report-facing roles. The one exception: the Security & Access
 * Log report stays on security:read - US-RPT-14's own AC refuses a Viewer,
 * consistent with US-SEC-11's live view.
 * <p>
 * Every report takes {@code format=json|csv} (US-RPT-12's CSV leg): json for
 * the generic frontend table, csv as a UTF-8-BOM RFC 4180 download.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final AdoptionReportService adoptionReportService;
    private final AdHocReportService adHocReportService;
    private final CsvExporter csvExporter;
    private final XlsxExporter xlsxExporter;
    private final PdfExporter pdfExporter;
    private final ExportJobService exportJobService;
    private final ReportDispatchService dispatchService;
    private final ReportScheduleService scheduleService;
    private final ReportScheduleJob scheduleJob;

    public ReportController(ReportService reportService, AdoptionReportService adoptionReportService,
                            AdHocReportService adHocReportService,
                            CsvExporter csvExporter, XlsxExporter xlsxExporter,
                            PdfExporter pdfExporter, ExportJobService exportJobService,
                            ReportDispatchService dispatchService, ReportScheduleService scheduleService,
                            ReportScheduleJob scheduleJob) {
        this.reportService = reportService;
        this.adoptionReportService = adoptionReportService;
        this.adHocReportService = adHocReportService;
        this.csvExporter = csvExporter;
        this.xlsxExporter = xlsxExporter;
        this.pdfExporter = pdfExporter;
        this.exportJobService = exportJobService;
        this.dispatchService = dispatchService;
        this.scheduleService = scheduleService;
        this.scheduleJob = scheduleJob;
    }

    @GetMapping("/asset-register")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "run-asset-register")
    public ResponseEntity<?> assetRegister(@RequestParam(required = false) UUID orgNodeId,
                                            @RequestParam(required = false) UUID categoryId,
                                            @RequestParam(required = false) UUID statusId,
                                            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.assetRegister(orgNodeId, categoryId, statusId), format);
    }

    @GetMapping("/employee-assets")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "run-employee-assets")
    public ResponseEntity<?> employeeAssets(@RequestParam UUID personId,
                                             @RequestParam(defaultValue = "json") String format) {
        return render(reportService.employeeAssets(personId), format);
    }

    @GetMapping("/expiry")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "run-expiry")
    public ResponseEntity<?> expiry(@RequestParam(defaultValue = "90") int withinDays,
                                     @RequestParam(defaultValue = "json") String format) {
        return render(reportService.expiry(withinDays), format);
    }

    @GetMapping("/asset-movements")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "run-asset-movements")
    public ResponseEntity<?> assetMovements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.assetMovements(from, to), format);
    }

    @GetMapping("/loss")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "run-loss")
    public ResponseEntity<?> loss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.loss(from, to), format);
    }

    @GetMapping("/vendor-purchases")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "run-vendor-purchases")
    public ResponseEntity<?> vendorPurchases(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.vendorPurchases(from, to), format);
    }

    @GetMapping("/audit-compliance")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "run-audit-compliance")
    public ResponseEntity<?> auditCompliance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.auditCompliance(from, to), format);
    }

    @GetMapping("/depreciation")
    // US-SEC-14 (AC-SEC-14-H): a human with reports:read OR a scoped INTEGRATION_SVC
    // service account holding INT_ACCOUNTING_READ. A service account can reach this and
    // nothing else: SecurityConfig default-denies integration principals on every path
    // except this one (its SERVICE_ACCOUNT_ENDPOINTS whitelist), and this OR-clause then
    // confirms the account actually holds the scope.
    @PreAuthorize("@perm.has('reports:read') or @svc.hasScope('INT_ACCOUNTING_READ')")
    @TrackUsage(module = "reports", action = "run-depreciation")
    public ResponseEntity<?> depreciation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.depreciation(asOf), format);
    }

    @GetMapping("/maintenance-history")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "run-maintenance-history")
    public ResponseEntity<?> maintenanceHistory(@RequestParam(required = false) UUID assetId,
                                                 @RequestParam(defaultValue = "json") String format) {
        return render(reportService.maintenanceHistory(assetId), format);
    }

    @GetMapping("/security-events")
    @PreAuthorize("@perm.has('security:read')")
    @TrackUsage(module = "reports", action = "run-security-events")
    public ResponseEntity<?> securityEvents(
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) SecurityEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.securityEvents(actorUserId, eventType, from, to), format);
    }

    /**
     * US-ANL-03: feature adoption by role x module. analytics:read-gated -
     * deliberately seeded to no role, so only a wildcard holder (Super
     * Administrator) or a future deliberate grant sees it.
     */
    @GetMapping("/usage-adoption")
    @PreAuthorize("@perm.has('analytics:read')")
    @TrackUsage(module = "reports", action = "run-usage-adoption")
    public ResponseEntity<?> usageAdoption(@RequestParam(defaultValue = "90") int withinDays,
                                            @RequestParam(defaultValue = "json") String format) {
        return render(adoptionReportService.usageAdoption(withinDays), format);
    }

    // US-RPT-12's background path: a very large export runs as a job with
    // progress instead of blocking. The per-key permission split lives in
    // ReportAccessPolicy - checked HERE on the request thread; the worker
    // thread then re-runs generation under the same delegated security context.
    @PostMapping("/{reportKey}/export-jobs")
    @PreAuthorize("@reportAccess.canRun(#reportKey)")
    @TrackUsage(module = "reports", action = "export-background")
    public ResponseEntity<ExportJobResponse> submitExportJob(@PathVariable String reportKey,
            @RequestParam(defaultValue = "xlsx") String exportFormat,
            @RequestParam Map<String, String> allParams) {
        dispatchService.requireKnownKey(reportKey);
        RenderFunction renderer = rendererFor(exportFormat);
        Map<String, String> params = new HashMap<>(allParams);
        params.remove("exportFormat");
        ExportJobService.ExportJob job =
                exportJobService.submit(reportKey, exportFormat.toLowerCase(), () -> dispatchService.generate(reportKey, params), renderer);
        return ResponseEntity.accepted().body(ExportJobResponse.from(job));
    }

    @GetMapping("/export-jobs/{id}")
    public ExportJobResponse exportJob(@PathVariable UUID id) {
        return ExportJobResponse.from(exportJobService.get(id));
    }

    @GetMapping("/export-jobs/{id}/download")
    public ResponseEntity<byte[]> downloadExportJob(@PathVariable UUID id) {
        ExportJobService.DownloadableExport export = exportJobService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.fileName() + "\"")
                .contentType(mediaTypeFor(export.format()))
                .body(export.content());
    }

    private RenderFunction rendererFor(String format) {
        return switch (format == null ? "" : format.toLowerCase()) {
            case "csv" -> csvExporter::export;
            case "xlsx" -> xlsxExporter::export;
            case "pdf" -> pdfExporter::export;
            default -> throw ValidationFailedException.singleField("exportFormat",
                    "exportFormat must be one of csv, xlsx, pdf - got " + format);
        };
    }

    private static MediaType mediaTypeFor(String format) {
        return switch (format) {
            case "csv" -> new MediaType("text", "csv");
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        };
    }

    public record ExportJobResponse(UUID id, String reportKey, String format, String status, int progress,
                                     String fileName, String error) {
        static ExportJobResponse from(ExportJobService.ExportJob job) {
            return new ExportJobResponse(job.getId(), job.getReportKey(), job.getFormat(), job.getStatus().name(),
                    job.getProgress(), job.getFileName(), job.getError());
        }
    }

    // --- US-RPT-15: ad hoc saved reports (own-rows-only, like saved searches) ---
    // All on reports:read: building a personal report grants nothing the
    // asset register doesn't already show - rows come from the same
    // org-scoped search, so a definition can never out-see its owner.

    @GetMapping("/ad-hoc/fields")
    @PreAuthorize("@perm.has('reports:read')")
    public List<AdHocReportService.FieldOption> adHocFields() {
        return adHocReportService.availableFields();
    }

    @PostMapping("/ad-hoc")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "adhoc-create")
    public ResponseEntity<AdHocReportResponse> createAdHoc(@Valid @RequestBody AdHocCreateRequest request) {
        AdHocReport report = adHocReportService.create(request.name(), request.fields(), request.query(),
                request.categoryId(), request.statusId(), request.orgNodeId(),
                request.purchasedFrom(), request.purchasedTo());
        return ResponseEntity.status(201).body(AdHocReportResponse.from(report));
    }

    @GetMapping("/ad-hoc")
    @PreAuthorize("@perm.has('reports:read')")
    public List<AdHocReportResponse> listAdHoc() {
        return adHocReportService.listOwn().stream().map(AdHocReportResponse::from).toList();
    }

    @GetMapping("/ad-hoc/{id}/run")
    @PreAuthorize("@perm.has('reports:read')")
    @TrackUsage(module = "reports", action = "run-adhoc")
    public ResponseEntity<?> runAdHoc(@PathVariable UUID id, @RequestParam(defaultValue = "json") String format) {
        return render(adHocReportService.run(id), format);
    }

    @DeleteMapping("/ad-hoc/{id}")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<Void> deleteAdHoc(@PathVariable UUID id) {
        adHocReportService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record AdHocCreateRequest(@NotBlank String name, @NotNull List<String> fields, String query,
                                      UUID categoryId, UUID statusId, UUID orgNodeId,
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchasedFrom,
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchasedTo) {
    }

    public record AdHocReportResponse(UUID id, String name, List<String> fields, String query, UUID categoryId,
                                       UUID statusId, UUID orgNodeId, LocalDate purchasedFrom, LocalDate purchasedTo) {
        static AdHocReportResponse from(AdHocReport r) {
            return new AdHocReportResponse(r.getId(), r.getName(), r.getFields(), r.getQuery(), r.getCategoryId(),
                    r.getStatusId(), r.getOrgNodeId(), r.getPurchasedFrom(), r.getPurchasedTo());
        }
    }

    // --- US-RPT-13: recurring delivery schedules (own-rows-only, like export jobs) ---

    @PostMapping("/{reportKey}/schedules")
    @PreAuthorize("@reportAccess.canRun(#reportKey)")
    @TrackUsage(module = "reports", action = "schedule")
    public ResponseEntity<ScheduleResponse> createSchedule(@PathVariable String reportKey,
            @Valid @RequestBody ScheduleCreateRequest request) {
        ReportSchedule schedule = scheduleService.create(reportKey, request.params(), request.exportFormat(),
                request.frequency(), request.recipients());
        return ResponseEntity.status(201).body(ScheduleResponse.from(schedule));
    }

    @GetMapping("/schedules")
    public List<ScheduleResponse> listSchedules() {
        return scheduleService.listOwn().stream().map(ScheduleResponse::from).toList();
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Ops/testing: run everything currently due, now, instead of waiting for the sweep. */
    @PostMapping("/admin/run-due-schedules")
    @PreAuthorize("@perm.has('notifications:manage')")
    public Map<String, Integer> runDueSchedules() {
        return Map.of("ran", scheduleJob.runDue());
    }

    public record ScheduleCreateRequest(Map<String, String> params, @NotBlank String exportFormat,
                                         @NotNull ReportSchedule.Frequency frequency,
                                         @NotNull List<String> recipients) {
    }

    public record ScheduleResponse(UUID id, String reportKey, String exportFormat, ReportSchedule.Frequency frequency,
                                    List<String> recipients, String status, java.time.Instant nextRunAt,
                                    java.time.Instant lastRunAt, String lastOutcome) {
        static ScheduleResponse from(ReportSchedule s) {
            return new ScheduleResponse(s.getId(), s.getReportKey(), s.getExportFormat(), s.getFrequency(),
                    List.of(s.getRecipients().split(",")), s.getStatus().name(), s.getNextRunAt(), s.getLastRunAt(),
                    s.getLastOutcome());
        }
    }

    private ResponseEntity<?> render(TabularReport report, String format) {
        return switch (format == null ? "json" : format.toLowerCase()) {
            case "csv" -> download(report, "csv", new MediaType("text", "csv"), csvExporter.export(report));
            case "xlsx" -> download(report, "xlsx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    xlsxExporter.export(report));
            case "pdf" -> download(report, "pdf", MediaType.APPLICATION_PDF, pdfExporter.export(report));
            case "json" -> ResponseEntity.ok(report);
            default -> throw ValidationFailedException.singleField("format",
                    "format must be one of json, csv, xlsx, pdf - got " + format);
        };
    }

    private ResponseEntity<byte[]> download(TabularReport report, String extension, MediaType mediaType, byte[] body) {
        String filename = report.key() + "-" + LocalDate.now() + "." + extension;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(body);
    }
}
