package com.iams.usr.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoleUpdateRequest(
        String name,
        String description,
        List<String> permissions,
        @NotNull Long version
) {
}
