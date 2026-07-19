package com.iams.org.api.dto;

import com.iams.org.domain.PersonType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PersonUpdateRequest(
        String fullName,
        String email,
        PersonType personType,
        UUID orgNodeId,
        UUID departmentId,
        Boolean active,
        @NotNull Long version
) {
}
