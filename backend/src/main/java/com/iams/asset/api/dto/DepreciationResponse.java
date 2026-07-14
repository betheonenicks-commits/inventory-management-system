package com.iams.asset.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DepreciationResponse(
        String status,
        String method,
        Integer usefulLifeMonths,
        BigDecimal salvageValue,
        BigDecimal monthlyDepreciation,
        BigDecimal accumulatedDepreciation,
        BigDecimal netBookValue,
        LocalDate asOf
) {
}
