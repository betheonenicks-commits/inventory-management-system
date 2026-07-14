package com.iams.asset.api.dto;

import com.iams.asset.domain.DepreciationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DepreciationOverrideResponse(
        UUID id,
        long version,
        UUID assetId,
        DepreciationMethod method,
        Integer usefulLifeMonths,
        BigDecimal salvageValuePct,
        LocalDate depreciationStartDate
) {
}
