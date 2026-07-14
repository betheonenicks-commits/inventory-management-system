package com.iams.org.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrgNodeCreateRequest(
        @NotBlank String name,
        @NotBlank String code,
        UUID parentId,
        @NotNull UUID levelId,
        String roomVariant
) {
}
