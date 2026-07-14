package com.iams.audit.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AuditRejectRequest(
        @NotBlank String reason
) {
}
