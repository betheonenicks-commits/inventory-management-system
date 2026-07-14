package com.iams.usr.api.dto;

import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        long version,
        String code,
        String name,
        String description,
        boolean system,
        boolean sensitive,
        boolean assignableToHumans,
        List<String> permissions
) {
}
