package com.iams.audit.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * US-AUD-18: one closed audit cycle's metrics for the cross-cycle trend view.
 * {@code missingRatePct} is the rate at close; {@code netMissingRatePct} credits
 * formal US-AUD-21 reconciliations (the reduction BO-2/BO-3 track).
 */
public record AuditCycleTrendResponse(
        UUID auditId,
        String name,
        Instant approvedAt,
        long expectedCount,
        long missingCount,
        long reconciledCount,
        long netMissingCount,
        double missingRatePct,
        double netMissingRatePct,
        Double completionDays
) {
}
