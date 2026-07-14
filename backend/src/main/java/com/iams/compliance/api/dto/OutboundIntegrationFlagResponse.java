package com.iams.compliance.api.dto;

import java.util.UUID;

public record OutboundIntegrationFlagResponse(UUID id, long version, String name, boolean enabled, String complianceReviewNote) {
}
