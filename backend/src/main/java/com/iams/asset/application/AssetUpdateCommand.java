package com.iams.asset.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Partial update (PATCH /assets/{id}). Every field is nullable/optional -
 * only non-null fields are applied - except `version`, which is mandatory
 * for the optimistic-locking check.
 */
public record AssetUpdateCommand(
        long version,
        String name,
        String manufacturer,
        String model,
        String serialNumber,
        String vendorName,
        String purchaseOrderReference,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,
        UUID orgNodeId,
        LocalDate warrantyStartDate,
        LocalDate warrantyEndDate,
        Map<String, Object> customFields
) {
}
