package com.iams.compliance.api.dto;

import com.iams.compliance.domain.RetentionEntityType;
import com.iams.compliance.domain.RetentionExpiryAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RetentionPolicyRequest(
        @NotNull RetentionEntityType entityType,
        @Positive int retentionPeriodDays,
        @NotNull RetentionExpiryAction expiryAction
) {
}
