package com.iams.sec.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

/** US-SEC-14: create a scoped integration service account. */
public record ServiceAccountCreateRequest(
        @NotBlank String name,
        String description,
        @NotEmpty Set<String> scopes
) {
}
