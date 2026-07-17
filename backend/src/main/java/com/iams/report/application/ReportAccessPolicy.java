package com.iams.report.application;

import com.iams.common.security.PermissionChecker;
import org.springframework.stereotype.Component;

/**
 * The one place that knows which permission runs which report key - used by
 * the key-parameterized endpoints (export jobs, schedules) whose
 * @PreAuthorize can't be a per-method literal. Keys split three ways:
 * security-events on security:read (US-RPT-14 refuses a Viewer),
 * usage-adoption on analytics:read (US-ANL-03 is Super-Admin-facing; the
 * permission is deliberately seeded to no role, so only the wildcard - or a
 * deliberate future grant - passes), everything else on reports:read.
 */
@Component("reportAccess")
public class ReportAccessPolicy {

    private final PermissionChecker perm;

    public ReportAccessPolicy(PermissionChecker perm) {
        this.perm = perm;
    }

    public boolean canRun(String reportKey) {
        return switch (reportKey == null ? "" : reportKey) {
            case ReportDispatchService.SECURITY_EVENTS_KEY -> perm.has("security:read");
            case ReportDispatchService.USAGE_ADOPTION_KEY -> perm.has("analytics:read");
            default -> perm.has("reports:read");
        };
    }
}
