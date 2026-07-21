package com.iams.common.security.api;

import jakarta.validation.constraints.NotBlank;

public record UnlockConfirmRequest(
        @NotBlank String token
) {
}
