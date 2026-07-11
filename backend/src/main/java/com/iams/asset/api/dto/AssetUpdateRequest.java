package com.iams.asset.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Partial update - every field except `version` is optional. `version` is
 * mandatory: the API spec requires every PATCH to echo the version it read,
 * so a stale write can be detected (409 OPTIMISTIC_LOCK_CONFLICT).
 */
public record AssetUpdateRequest(
        @NotNull Long version,
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
