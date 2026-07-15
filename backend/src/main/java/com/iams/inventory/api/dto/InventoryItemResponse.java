package com.iams.inventory.api.dto;

import com.iams.inventory.domain.CostingMethod;
import com.iams.inventory.domain.UnitOfMeasure;
import java.math.BigDecimal;
import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        long version,
        String name,
        String sku,
        UnitOfMeasure unitOfMeasure,
        BigDecimal reorderLevel,
        CostingMethod costingMethod,
        boolean active
) {
}
