package com.iams.audit.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditCertificateResponse(
        UUID auditId,
        String auditName,
        long expectedCount,
        long verifiedCount,
        long missingCount,
        long damagedCount,
        UUID approvedBy,
        String approverName,
        Instant approvedAt
) {
}
