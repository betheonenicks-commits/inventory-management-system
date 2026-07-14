package com.iams.compliance.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LegalHoldLiftRequest(@NotBlank String liftReason) {
}
