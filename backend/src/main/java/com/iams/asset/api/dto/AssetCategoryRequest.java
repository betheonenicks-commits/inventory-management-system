package com.iams.asset.api.dto;

import com.iams.asset.domain.DepreciationMethod;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

public record AssetCategoryRequest(
        @NotBlank String name,
        @NotBlank String code,
        Boolean active,
        Long version,
        List<CustomFieldDefinitionRequest> customFields,
        Boolean requiresVehicleFields,
        DepreciationMethod defaultDepreciationMethod,
        Integer defaultUsefulLifeMonths,
        BigDecimal defaultSalvageValuePct
) {
}
