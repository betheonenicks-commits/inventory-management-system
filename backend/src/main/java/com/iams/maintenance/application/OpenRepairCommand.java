package com.iams.maintenance.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** US-LIF-06: open a repair event. */
public record OpenRepairCommand(
        UUID assetId,
        String vendorName,
        String reason,
        BigDecimal estimatedCost,
        LocalDate expectedReturnDate
) {
}
