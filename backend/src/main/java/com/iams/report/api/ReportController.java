package com.iams.report.api;

import com.iams.report.application.CsvExporter;
import com.iams.report.application.ReportService;
import com.iams.report.application.TabularReport;
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

    public ReportController(ReportService reportService, CsvExporter csvExporter) {
        this.reportService = reportService;
        this.csvExporter = csvExporter;
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

    private ResponseEntity<?> render(TabularReport report, String format) {
        if ("csv".equalsIgnoreCase(format)) {
            String filename = report.key() + "-" + LocalDate.now() + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(new MediaType("text", "csv"))
                    .body(csvExporter.export(report));
        }
        return ResponseEntity.ok(report);
    }
}
