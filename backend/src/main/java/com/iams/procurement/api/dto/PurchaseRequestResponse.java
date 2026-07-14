package com.iams.procurement.api.dto;

import com.iams.lifecycle.domain.LifecycleRequestStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseRequestResponse(
        UUID id,
        long version,
        String itemDescription,
        String justification,
        BigDecimal estimatedCost,
        String vendorName,
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
