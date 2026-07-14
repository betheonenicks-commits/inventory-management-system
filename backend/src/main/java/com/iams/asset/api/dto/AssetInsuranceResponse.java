package com.iams.asset.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AssetInsuranceResponse(
        UUID id,
        long version,
        UUID assetId,
        String insurerName,
        String policyNumber,
        BigDecimal coverageAmount,
        String coverageCurrency,
        LocalDate policyStartDate,
        LocalDate policyExpiryDate,
        boolean expired
) {
}
