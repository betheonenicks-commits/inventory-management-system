package com.iams.compliance.api.dto;

import jakarta.validation.constraints.NotBlank;

public record OutboundIntegrationFlagRequest(@NotBlank String name, boolean enabled, String complianceReviewNote) {
}
