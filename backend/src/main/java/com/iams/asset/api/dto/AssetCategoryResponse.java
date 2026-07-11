package com.iams.asset.api.dto;

import java.util.List;
import java.util.UUID;

public record AssetCategoryResponse(
        UUID id,
        String name,
        String code,
        boolean active,
        long version,
        List<CustomFieldDefinitionResponse> customFields
) {
}
