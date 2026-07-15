package com.iams.inventory.api.dto;

import com.iams.inventory.domain.CostingMethod;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record InventoryItemUpdateRequest(
        @NotBlank String name,
        BigDecimal reorderLevel,
        CostingMethod costingMethod
) {
}
