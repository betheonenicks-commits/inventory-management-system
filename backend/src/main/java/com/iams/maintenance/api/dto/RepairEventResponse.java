package com.iams.maintenance.api.dto;

import com.iams.maintenance.domain.RepairEventStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RepairEventResponse(
        UUID id,
        long version,
        UUID assetId,
        String assetNumber,
        String previousStatusCode,
        String vendorName,
        String reason,
        BigDecimal estimatedCost,
        LocalDate expectedReturnDate,
        BigDecimal actualCost,
        LocalDate actualReturnDate,
        RepairEventStatus status,
        UUID loggedBy
) {
}
