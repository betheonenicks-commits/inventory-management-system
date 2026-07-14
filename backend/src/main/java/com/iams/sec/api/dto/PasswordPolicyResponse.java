package com.iams.sec.api.dto;

import java.util.UUID;

public record PasswordPolicyResponse(
        UUID id,
        long version,
        int minLength,
        boolean requireUppercase,
        boolean requireLowercase,
        boolean requireDigit,
        boolean requireSpecial
) {
}
