package com.iams.audit.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AuditAssignmentRequest(
        @NotNull UUID auditorUserId,
        String subScope
) {
}
