package com.iams.inventory.api.dto;

import com.iams.inventory.domain.CostingMethod;
import com.iams.inventory.domain.UnitOfMeasure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InventoryItemCreateRequest(
        @NotBlank String name,
        @NotBlank String sku,
        @NotNull UnitOfMeasure unitOfMeasure,
        BigDecimal reorderLevel,
        CostingMethod costingMethod
) {
}
