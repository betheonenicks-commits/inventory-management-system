package com.iams.inventory.api.dto;

import com.iams.inventory.domain.CostingMethod;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemCostingMethodChangeResponse(
        UUID id,
        UUID itemId,
        CostingMethod oldMethod,
        CostingMethod newMethod,
        UUID changedBy,
        Instant changedAt
) {
}
