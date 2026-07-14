package com.iams.audit.api.dto;

import com.iams.audit.domain.CorrectionField;
import java.time.Instant;
import java.util.UUID;

public record AuditFindingCorrectionResponse(
        UUID id,
        CorrectionField fieldName,
        String oldValue,
        String newValue,
        UUID actorId,
        String actorUsername,
        Instant createdAt
) {
}
