package com.iams.asset.api.dto;

import com.iams.asset.domain.DepreciationMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AssetCategoryResponse(
        UUID id,
        String name,
        String code,
        boolean active,
        long version,
        List<CustomFieldDefinitionResponse> customFields,
        boolean requiresVehicleFields,
        DepreciationMethod defaultDepreciationMethod,
        Integer defaultUsefulLifeMonths,
        BigDecimal defaultSalvageValuePct
) {
}
