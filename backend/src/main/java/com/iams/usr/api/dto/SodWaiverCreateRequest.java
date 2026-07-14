package com.iams.usr.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SodWaiverCreateRequest(
        @NotBlank String scope,
        @NotNull UUID signedOffByUserId,
        @NotBlank String reason
) {
}
