package com.iams.common.security.api;

import jakarta.validation.constraints.NotBlank;

public record StepUpRequest(
        @NotBlank String password
) {
}
