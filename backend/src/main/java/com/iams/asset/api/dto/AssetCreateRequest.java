package com.iams.asset.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record AssetCreateRequest(
        @NotNull UUID categoryId,
        @NotBlank String name,
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
        String rfidTagId,
        Map<String, Object> customFields
) {
}
