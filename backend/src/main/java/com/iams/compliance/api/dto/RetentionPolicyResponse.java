package com.iams.compliance.api.dto;

import com.iams.compliance.domain.RetentionEntityType;
import com.iams.compliance.domain.RetentionExpiryAction;
import java.util.UUID;

public record RetentionPolicyResponse(
        UUID id,
        long version,
        RetentionEntityType entityType,
        int retentionPeriodDays,
        RetentionExpiryAction expiryAction
) {
}
