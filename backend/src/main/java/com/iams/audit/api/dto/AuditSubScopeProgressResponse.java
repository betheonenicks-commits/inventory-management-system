package com.iams.audit.api.dto;

import java.util.UUID;

/**
 * US-AUD-03: one sub-scope (org node / location) of a bulk audit, with its own
 * expected-vs-found counts and completion percentage - so a wide-scope audit's
 * progress reads as a breakdown, not one flat total.
 */
public record AuditSubScopeProgressResponse(
        UUID orgNodeId,
        String orgNodeName,
        String orgNodeCode,
        long expectedCount,
        long verifiedCount,
        long missingCount,
        long outOfScopeCount,
        long scopeChangedCount,
        double percentComplete
) {
}
