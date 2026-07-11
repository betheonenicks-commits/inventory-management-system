package com.iams.asset.api.dto;

import java.util.List;
import java.util.UUID;

public record CustomFieldDefinitionResponse(
        UUID id,
        String fieldKey,
        String label,
        String dataType,
        boolean required,
        List<String> enumOptions,
        int displayOrder
) {
}
