package com.iams.compliance.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PersonAnonymizationResponse(UUID id, String fullName, boolean active, Instant anonymizedAt) {
}
