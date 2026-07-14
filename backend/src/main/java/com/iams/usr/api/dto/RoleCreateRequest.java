package com.iams.usr.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RoleCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        List<String> permissions
) {
}
