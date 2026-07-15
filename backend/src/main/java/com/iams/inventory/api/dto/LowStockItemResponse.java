package com.iams.inventory.api.dto;

import com.iams.inventory.domain.UnitOfMeasure;
import java.math.BigDecimal;
import java.util.UUID;

public record LowStockItemResponse(
        UUID itemId,
        String name,
        String sku,
        UnitOfMeasure unitOfMeasure,
        BigDecimal reorderLevel,
        BigDecimal totalQuantity
) {
}
