package com.iams.audit.api.dto;

import com.iams.audit.domain.CorrectionField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuditCorrectionRequest(
        @NotNull CorrectionField fieldName,
        @NotBlank String newValue
) {
}
