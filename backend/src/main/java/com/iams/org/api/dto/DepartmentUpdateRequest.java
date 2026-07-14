package com.iams.org.api.dto;

import jakarta.validation.constraints.NotNull;

public record DepartmentUpdateRequest(
        String name,
        String costCenterCode,
        Boolean active,
        @NotNull Long version
) {
}
