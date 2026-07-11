package com.iams.asset.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AssetCategoryRequest(
        @NotBlank String name,
        @NotBlank String code,
        Boolean active,
        Long version,
        List<CustomFieldDefinitionRequest> customFields
) {
}
