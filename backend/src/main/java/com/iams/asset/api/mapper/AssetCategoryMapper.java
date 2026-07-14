package com.iams.asset.api.mapper;

import com.iams.asset.api.dto.AssetCategoryResponse;
import com.iams.asset.api.dto.CustomFieldDefinitionResponse;
import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetCustomFieldDefinition;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AssetCategoryMapper {

    public AssetCategoryResponse toResponse(AssetCategory category, List<AssetCustomFieldDefinition> fields) {
        return new AssetCategoryResponse(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.isActive(),
                category.getVersion(),
                fields.stream().map(this::toFieldResponse).toList(),
                category.isRequiresVehicleFields(),
                category.getDefaultDepreciationMethod(),
                category.getDefaultUsefulLifeMonths(),
                category.getDefaultSalvageValuePct()
        );
    }

    public CustomFieldDefinitionResponse toFieldResponse(AssetCustomFieldDefinition def) {
        return new CustomFieldDefinitionResponse(
                def.getId(),
                def.getFieldKey(),
                def.getLabel(),
                def.getDataType().name(),
                def.isRequired(),
                def.getEnumOptions(),
                def.getDisplayOrder()
        );
    }
}
