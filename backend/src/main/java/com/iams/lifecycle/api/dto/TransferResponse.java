package com.iams.lifecycle.api.dto;

import com.iams.lifecycle.domain.LifecycleRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        long version,
        UUID assetId,
        String assetNumber,
        UUID fromOrgNodeId,
        String fromOrgNodeCode,
        UUID toOrgNodeId,
        String toOrgNodeCode,
        UUID fromPersonId,
        UUID toPersonId,
        String reason,
        LifecycleRequestStatus status,
        UUID nominalApproverId,
        UUID effectiveApproverId,
        UUID requestedBy,
        Instant requestedAt,
        UUID decidedBy,
        Instant decidedAt,
        String rejectionReason
) {
}
