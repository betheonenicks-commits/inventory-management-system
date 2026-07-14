package com.iams.audit.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AuditSubmitRequest(
        @NotBlank String password,
        String signatureName
) {
}
