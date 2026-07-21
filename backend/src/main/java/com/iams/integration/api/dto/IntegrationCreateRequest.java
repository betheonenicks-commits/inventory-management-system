package com.iams.integration.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * US-SEC-15: registering an integration. {@code credentialRef} must be a secrets-manager
 * reference (e.g. {@code vault:path#key}, {@code env:NAME}) - the service rejects an inline
 * secret here, or one smuggled into {@code config}, with 400 VALIDATION_FAILED (AC-SEC-15-X).
 */
public record IntegrationCreateRequest(
        @NotBlank String name,
        @NotBlank String type,
        String description,
        String credentialRef,
        Map<String, String> config
) {
}
