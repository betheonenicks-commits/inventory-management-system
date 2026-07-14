package com.iams.lifecycle.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ApprovalDelegationResponse(
        UUID id,
        long version,
        UUID delegatorUserId,
        UUID delegateUserId,
        Instant validFrom,
        Instant validTo,
        boolean active,
        String reason
) {
}
