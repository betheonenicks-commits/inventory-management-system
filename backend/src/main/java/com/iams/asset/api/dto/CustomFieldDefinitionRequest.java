package com.iams.asset.api.dto;

import com.iams.asset.domain.CustomFieldDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CustomFieldDefinitionRequest(
        @NotBlank String fieldKey,
        @NotBlank String label,
        @NotNull CustomFieldDataType dataType,
        boolean required,
        List<String> enumOptions
) {
}
