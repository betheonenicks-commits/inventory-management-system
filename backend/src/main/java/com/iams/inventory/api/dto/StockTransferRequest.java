package com.iams.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record StockTransferRequest(
        @NotNull UUID itemId,
        @NotNull UUID fromWarehouseId,
        String fromSubLocation,
        String fromLotNumber,
        @NotNull UUID toWarehouseId,
        String toSubLocation,
        String toLotNumber,
        @NotNull BigDecimal quantity,
        @NotBlank String reasonCode
) {
}
