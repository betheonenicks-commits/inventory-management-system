package com.iams.sec.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SecurityEventResponse(
        UUID id,
        String eventType,
        UUID actorUserId,
        String usernameAttempted,
        String ipAddress,
        String detail,
        Instant createdAt
) {
}
