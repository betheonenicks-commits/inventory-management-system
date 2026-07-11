package com.iams.asset.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        String assetNumber,
        long version,
        String name,
        UUID categoryId,
        String categoryName,
        AssetStatusInfo status,
        UUID orgNodeId,
        String orgNodeName,
        UUID assignedToPersonId,
        UUID parentAssetId,
        String serialNumber,
        String manufacturer,
        String model,
        String description,
        BarcodeInfo barcode,
        QrCodeInfo qrCode,
        String vendorName,
        String purchaseOrderReference,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,
        LocalDate warrantyStartDate,
        LocalDate warrantyEndDate,
        Map<String, Object> customFields,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
