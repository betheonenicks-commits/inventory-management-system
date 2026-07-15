package com.iams.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StockInRequest(
        @NotNull UUID itemId,
        @NotNull UUID warehouseId,
        String subLocation,
        String lotNumber,
        LocalDate expiryDate,
        @NotNull BigDecimal quantity,
        @NotNull BigDecimal unitCost,
        String currencyCode,
        BigDecimal fxRate,
        @NotBlank String reasonCode
) {
}
