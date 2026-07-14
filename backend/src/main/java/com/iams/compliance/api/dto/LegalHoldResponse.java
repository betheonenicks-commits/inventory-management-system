package com.iams.compliance.api.dto;

import com.iams.compliance.domain.LegalHoldScopeType;
import java.time.Instant;
import java.util.UUID;

public record LegalHoldResponse(
        UUID id,
        long version,
        LegalHoldScopeType scopeType,
        UUID scopeId,
        String reason,
        boolean active,
        UUID liftedBy,
        Instant liftedAt,
        String liftReason
) {
}
