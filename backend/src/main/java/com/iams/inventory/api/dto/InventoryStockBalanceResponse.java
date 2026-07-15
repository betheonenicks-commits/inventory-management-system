package com.iams.inventory.api.dto;

import com.iams.inventory.domain.UnitOfMeasure;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryStockBalanceResponse(
        UUID id,
        UUID itemId,
        String itemName,
        String sku,
        UnitOfMeasure unitOfMeasure,
        UUID warehouseId,
        String warehouseName,
        String subLocation,
        String lotNumber,
        LocalDate expiryDate,
        BigDecimal quantityOnHand,
        BigDecimal averageUnitCost,
        long version
) {
}
