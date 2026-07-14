package com.iams.usr.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SodWaiverResponse(
        UUID id,
        long version,
        String scope,
        UUID signedOffByUserId,
        String signedOffByUsername,
        String reason,
        boolean active,
        UUID createdBy,
        Instant createdAt
) {
}
