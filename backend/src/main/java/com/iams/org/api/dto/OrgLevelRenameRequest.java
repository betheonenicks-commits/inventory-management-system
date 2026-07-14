package com.iams.org.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrgLevelRenameRequest(
        @NotBlank String name,
        @NotNull Long version
) {
}
