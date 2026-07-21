package com.iams.integration.api.dto;

import com.iams.integration.domain.Integration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * US-SEC-15: an integration as shown to admins. {@code credentialRef} is safe to expose - it
 * is a secrets-manager <em>reference</em>, not the secret itself (AC-SEC-15-H); there is no
 * plaintext credential to hide because none is ever stored.
 */
public record IntegrationResponse(
        UUID id,
        String name,
        String type,
        String description,
        String credentialRef,
        Map<String, String> config,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static IntegrationResponse from(Integration i) {
        return new IntegrationResponse(i.getId(), i.getName(), i.getType().name(), i.getDescription(),
                i.getCredentialRef(), i.getConfig(), i.isEnabled(), i.getCreatedAt(), i.getUpdatedAt());
    }
}
