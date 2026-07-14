package com.iams.lifecycle.api.dto;

import com.iams.lifecycle.domain.DisposalType;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record DisposalResponse(
        UUID id,
        long version,
        UUID assetId,
        String assetNumber,
        DisposalType disposalType,
        String reason,
        LifecycleRequestStatus status,
        UUID nominalApproverId,
        UUID effectiveApproverId,
        UUID requestedBy,
        Instant requestedAt,
        UUID decidedBy,
        Instant decidedAt,
        String rejectionReason,
        Instant restoredAt,
        UUID restoredBy
) {
}
