package com.iams.asset.api.dto;

import com.iams.asset.domain.DepreciationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DepreciationOverrideRequest(
        DepreciationMethod method,
        Integer usefulLifeMonths,
        BigDecimal salvageValuePct,
        LocalDate depreciationStartDate,
        Long version
) {
}
