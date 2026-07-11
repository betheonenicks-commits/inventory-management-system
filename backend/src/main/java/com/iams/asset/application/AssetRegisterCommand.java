package com.iams.asset.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Create-asset request, mirroring the API spec's POST /assets body exactly.
 */
public record AssetRegisterCommand(
        UUID categoryId,
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
