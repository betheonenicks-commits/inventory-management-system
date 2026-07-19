package com.iams.audit.api.dto;

import java.util.List;

public record AuditProgressResponse(
        long expectedCount,
        long verifiedCount,
        long missingCount,
        long outOfScopeCount,
        long scopeChangedCount,
        double percentComplete,
        // US-AUD-03: per-sub-scope (per-location) breakdown of the same counts.
        // Empty where a breakdown isn't computed (e.g. the dashboard's per-audit tile).
        List<AuditSubScopeProgressResponse> subScopes
) {
}
