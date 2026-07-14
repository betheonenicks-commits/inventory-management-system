package com.iams.org.api.dto;

import com.iams.org.domain.PersonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PersonCreateRequest(
        @NotBlank String fullName,
        String email,
        @NotNull PersonType personType,
        UUID orgNodeId
) {
}
