package com.iams.audit.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditFindingReconciliationResponse(
        UUID id,
        UUID findingId,
        String foundLocationNote,
        UUID reconciledByUserId,
        String reconciledByUsername,
        Instant reconciledAt
) {
}
