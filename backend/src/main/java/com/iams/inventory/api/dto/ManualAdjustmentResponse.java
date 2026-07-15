package com.iams.inventory.api.dto;

import com.iams.lifecycle.domain.LifecycleRequestStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ManualAdjustmentResponse(
        UUID id,
        long version,
        UUID itemId,
        String itemName,
        UUID warehouseId,
        String warehouseName,
        String subLocation,
        String lotNumber,
        BigDecimal quantityDelta,
        String reason,
        LifecycleRequestStatus status,
        UUID nominalApproverId,
        UUID effectiveApproverId,
        UUID requestedBy,
        Instant requestedAt,
        UUID decidedBy,
        Instant decidedAt,
        String rejectionReason,
        UUID resultingTransactionId
) {
}
