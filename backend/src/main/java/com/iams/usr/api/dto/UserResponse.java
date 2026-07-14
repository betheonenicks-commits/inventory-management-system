package com.iams.usr.api.dto;

import com.iams.usr.domain.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        long version,
        String username,
        String displayName,
        String email,
        UUID personId,
        UUID orgScopeNodeId,
        String orgScopeNodeName,
        UserStatus status,
        Set<String> roleCodes,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
