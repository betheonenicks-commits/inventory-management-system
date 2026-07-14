package com.iams.org.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        long version,
        String name,
        String costCenterCode,
        boolean active,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
