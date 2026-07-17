package com.iams.report.application;

import com.iams.common.exception.ValidationFailedException;
import com.iams.sec.domain.SecurityEventType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Regenerates any report from its key + raw string params - the background
 * export path's (US-RPT-12) way of re-running exactly what a foreground
 * GET /reports/{key} would, without duplicating any business logic (every
 * branch calls the same ReportService method the typed endpoint calls).
 * Unknown keys and malformed params are 400s, not 500s.
 */
@Service
public class ReportDispatchService {

    /** Rides on security:read instead of reports:read - see ReportAccessPolicy. */
    public static final String SECURITY_EVENTS_KEY = "security-events";

    /** Rides on analytics:read (US-ANL-03) - see ReportAccessPolicy. */
    public static final String USAGE_ADOPTION_KEY = "usage-adoption";

    private static final java.util.Set<String> KNOWN_KEYS = java.util.Set.of("asset-register", "employee-assets",
            "expiry", "asset-movements", "loss", "vendor-purchases", "audit-compliance", "depreciation",
            "maintenance-history", SECURITY_EVENTS_KEY, USAGE_ADOPTION_KEY);

    /** Fail a bad key at submit time (400) rather than birthing a job doomed to FAIL asynchronously. */
    public void requireKnownKey(String key) {
        if (!KNOWN_KEYS.contains(key)) {
            throw ValidationFailedException.singleField("report", "Unknown report key: " + key);
        }
    }

    private final ReportService reportService;
    private final AdoptionReportService adoptionReportService;

    public ReportDispatchService(ReportService reportService, AdoptionReportService adoptionReportService) {
        this.reportService = reportService;
        this.adoptionReportService = adoptionReportService;
    }

    public TabularReport generate(String key, Map<String, String> params) {
        return switch (key) {
            case "asset-register" -> reportService.assetRegister(
                    uuid(params, "orgNodeId"), uuid(params, "categoryId"), uuid(params, "statusId"));
            case "employee-assets" -> reportService.employeeAssets(requiredUuid(params, "personId"));
            case "expiry" -> reportService.expiry(intOrDefault(params, "withinDays", 90));
            case "asset-movements" -> reportService.assetMovements(
                    requiredDate(params, "from"), requiredDate(params, "to"));
            case "loss" -> reportService.loss(date(params, "from"), date(params, "to"));
            case "vendor-purchases" -> reportService.vendorPurchases(date(params, "from"), date(params, "to"));
            case "audit-compliance" -> reportService.auditCompliance(date(params, "from"), date(params, "to"));
            case "depreciation" -> reportService.depreciation(date(params, "asOf"));
            case "maintenance-history" -> reportService.maintenanceHistory(uuid(params, "assetId"));
            case SECURITY_EVENTS_KEY -> reportService.securityEvents(uuid(params, "actorUserId"),
                    enumOrNull(params.get("eventType")), instant(params, "from"), instant(params, "to"));
            case USAGE_ADOPTION_KEY -> adoptionReportService.usageAdoption(intOrDefault(params, "withinDays", 90));
            default -> throw ValidationFailedException.singleField("report", "Unknown report key: " + key);
        };
    }

    private static UUID uuid(Map<String, String> params, String name) {
        String value = params.get(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw ValidationFailedException.singleField(name, "must be a UUID");
        }
    }

    private static UUID requiredUuid(Map<String, String> params, String name) {
        UUID value = uuid(params, name);
        if (value == null) {
            throw ValidationFailedException.singleField(name, "is required for this report");
        }
        return value;
    }

    private static LocalDate date(Map<String, String> params, String name) {
        String value = params.get(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ValidationFailedException.singleField(name, "must be an ISO date (yyyy-MM-dd)");
        }
    }

    private static LocalDate requiredDate(Map<String, String> params, String name) {
        LocalDate value = date(params, name);
        if (value == null) {
            throw ValidationFailedException.singleField(name, "is required for this report");
        }
        return value;
    }

    private static Instant instant(Map<String, String> params, String name) {
        String value = params.get(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw ValidationFailedException.singleField(name, "must be an ISO instant");
        }
    }

    private static int intOrDefault(Map<String, String> params, String name, int fallback) {
        String value = params.get(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ValidationFailedException.singleField(name, "must be an integer");
        }
    }

    private static SecurityEventType enumOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return SecurityEventType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw ValidationFailedException.singleField("eventType", "unknown event type");
        }
    }
}
