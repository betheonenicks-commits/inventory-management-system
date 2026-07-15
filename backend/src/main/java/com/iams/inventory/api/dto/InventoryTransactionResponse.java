package com.iams.inventory.api.dto;

import com.iams.inventory.domain.InventoryTransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryTransactionResponse(
        UUID id,
        UUID itemId,
        String itemName,
        UUID warehouseId,
        String warehouseName,
        String subLocation,
        String lotNumber,
        LocalDate expiryDate,
        InventoryTransactionType transactionType,
        BigDecimal quantity,
        BigDecimal unitCost,
        String currencyCode,
        BigDecimal fxRate,
        BigDecimal reportingUnitCost,
        String reasonCode,
        UUID performedByUserId,
        String performedByUsername,
        Instant performedAt,
        UUID linkedTransactionId
) {
}
