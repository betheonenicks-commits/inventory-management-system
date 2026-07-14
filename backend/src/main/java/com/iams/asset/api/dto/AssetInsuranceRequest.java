package com.iams.asset.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetInsuranceRequest(
        String insurerName,
        String policyNumber,
        BigDecimal coverageAmount,
        String coverageCurrency,
        LocalDate policyStartDate,
        LocalDate policyExpiryDate,
        Long version
) {
}
