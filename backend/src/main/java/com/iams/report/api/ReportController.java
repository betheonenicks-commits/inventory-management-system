package com.iams.report.api;

import com.iams.common.exception.ValidationFailedException;
import com.iams.report.application.CsvExporter;
import com.iams.report.application.ExportJobService;
import com.iams.report.application.ExportJobService.RenderFunction;
import com.iams.report.application.PdfExporter;
import com.iams.report.application.ReportDispatchService;
import com.iams.report.application.ReportService;
import com.iams.report.application.TabularReport;
import com.iams.report.application.XlsxExporter;
import java.util.HashMap;
import java.util.Map;
import com.iams.sec.domain.SecurityEventType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final CsvExporter csvExporter;
    private final XlsxExporter xlsxExporter;
    private final PdfExporter pdfExporter;
    private final ExportJobService exportJobService;
    private final ReportDispatchService dispatchService;

    public ReportController(ReportService reportService, CsvExporter csvExporter, XlsxExporter xlsxExporter,
                            PdfExporter pdfExporter, ExportJobService exportJobService,
                            ReportDispatchService dispatchService) {
        this.reportService = reportService;
        this.csvExporter = csvExporter;
        this.xlsxExporter = xlsxExporter;
        this.pdfExporter = pdfExporter;
        this.exportJobService = exportJobService;
        this.dispatchService = dispatchService;
    }

    @GetMapping("/asset-register")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<?> assetRegister(@RequestParam(required = false) UUID orgNodeId,
                                            @RequestParam(required = false) UUID categoryId,
                                            @RequestParam(required = false) UUID statusId,
                                            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.assetRegister(orgNodeId, categoryId, statusId), format);
    }

    @GetMapping("/employee-assets")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<?> employeeAssets(@RequestParam UUID personId,
                                             @RequestParam(defaultValue = "json") String format) {
        return render(reportService.employeeAssets(personId), format);
    }

    @GetMapping("/expiry")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<?> expiry(@RequestParam(defaultValue = "90") int withinDays,
                                     @RequestParam(defaultValue = "json") String format) {
        return render(reportService.expiry(withinDays), format);
    }

    @GetMapping("/asset-movements")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<?> assetMovements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.assetMovements(from, to), format);
    }

    @GetMapping("/loss")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<?> loss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.loss(from, to), format);
    }

    @GetMapping("/vendor-purchases")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<?> vendorPurchases(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.vendorPurchases(from, to), format);
    }

    @GetMapping("/audit-compliance")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<?> auditCompliance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.auditCompliance(from, to), format);
    }

    @GetMapping("/depreciation")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<?> depreciation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.depreciation(asOf), format);
    }

    @GetMapping("/maintenance-history")
    @PreAuthorize("@perm.has('reports:read')")
    public ResponseEntity<?> maintenanceHistory(@RequestParam(required = false) UUID assetId,
                                                 @RequestParam(defaultValue = "json") String format) {
        return render(reportService.maintenanceHistory(assetId), format);
    }

    @GetMapping("/security-events")
    @PreAuthorize("@perm.has('security:read')")
    public ResponseEntity<?> securityEvents(
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) SecurityEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "json") String format) {
        return render(reportService.securityEvents(actorUserId, eventType, from, to), format);
    }

    // US-RPT-12's background path: a very large export runs as a job with
    // progress instead of blocking. The permission split mirrors the typed
    // endpoints: security-events requires security:read, everything else
    // reports:read - checked HERE on the request thread; the worker thread
    // then re-runs generation under the same delegated security context.
    @PostMapping("/{reportKey}/export-jobs")
    @PreAuthorize("#reportKey == T(com.iams.report.application.ReportDispatchService).SECURITY_EVENTS_KEY "
            + "? @perm.has('security:read') : @perm.has('reports:read')")
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
