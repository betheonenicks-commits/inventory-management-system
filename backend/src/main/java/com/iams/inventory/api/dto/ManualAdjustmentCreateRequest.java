package com.iams.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ManualAdjustmentCreateRequest(
        @NotNull UUID itemId,
        @NotNull UUID warehouseId,
        String subLocation,
        String lotNumber,
        @NotNull BigDecimal quantityDelta,
        @NotBlank String reason,
        @NotNull UUID nominalApproverId
) {
}
