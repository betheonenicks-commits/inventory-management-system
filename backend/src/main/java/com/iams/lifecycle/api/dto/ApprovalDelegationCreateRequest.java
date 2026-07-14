package com.iams.lifecycle.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record ApprovalDelegationCreateRequest(
        @NotNull UUID delegateUserId,
        @NotNull Instant validFrom,
        @NotNull Instant validTo,
        String reason
) {
}
