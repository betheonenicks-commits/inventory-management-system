package com.iams.usr.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String displayName,
        String email,
        UUID personId,
        UUID orgScopeNodeId,
        @NotEmpty Set<String> roleCodes
) {
}
