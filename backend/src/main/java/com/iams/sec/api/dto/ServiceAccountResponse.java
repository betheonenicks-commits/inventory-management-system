package com.iams.sec.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * US-SEC-14: a service account as shown to admins. Never carries the API key or its
 * hash - only the non-secret {@code apiKeyPrefix} for recognition.
 */
public record ServiceAccountResponse(
        UUID id,
        String name,
        String description,
        String apiKeyPrefix,
        Set<String> scopes,
        boolean active,
        Instant lastUsedAt,
        Instant createdAt
) {
}
