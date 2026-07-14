package com.iams.org.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentCreateRequest(
        @NotBlank String name,
        @NotBlank String costCenterCode
) {
}
