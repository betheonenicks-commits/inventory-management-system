package com.iams.maintenance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RepairOpenRequest(
        @NotNull UUID assetId,
        String vendorName,
        @NotBlank String reason,
        BigDecimal estimatedCost,
        LocalDate expectedReturnDate
) {
}
