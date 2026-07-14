package com.iams.audit.api.dto;

public record AuditProgressResponse(
        long expectedCount,
        long verifiedCount,
        long missingCount,
        long outOfScopeCount,
        long scopeChangedCount,
        double percentComplete
) {
}
