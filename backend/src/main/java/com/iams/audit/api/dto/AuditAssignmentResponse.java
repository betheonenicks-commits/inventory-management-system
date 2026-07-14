package com.iams.audit.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditAssignmentResponse(
        UUID id,
        UUID auditId,
        UUID auditorUserId,
        String auditorUsername,
        String subScope,
        boolean active,
        Instant unassignedAt,
        long version
) {
}
