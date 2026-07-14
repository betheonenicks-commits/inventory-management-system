package com.iams.usr.api.dto;

import jakarta.validation.constraints.NotNull;

public record UserDeactivateRequest(
        @NotNull Long version
) {
}
