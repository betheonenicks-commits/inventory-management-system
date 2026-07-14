package com.iams.sec.api.dto;

import jakarta.validation.constraints.NotNull;

public record PasswordPolicyUpdateRequest(
        Integer minLength,
        Boolean requireUppercase,
        Boolean requireLowercase,
        Boolean requireDigit,
        Boolean requireSpecial,
        @NotNull Long version
) {
}
