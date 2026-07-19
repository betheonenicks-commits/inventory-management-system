package com.iams.org.api.dto;

import com.iams.org.domain.PersonType;
import java.time.Instant;
import java.util.UUID;

public record PersonResponse(
        UUID id,
        long version,
        String fullName,
        String email,
        PersonType personType,
        UUID orgNodeId,
        String orgNodeName,
        UUID departmentId,
        boolean active,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
