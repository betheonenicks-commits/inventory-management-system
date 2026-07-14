package com.iams.asset.application;

import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.asset.domain.AssetCustomFieldDefinition;
import com.iams.asset.domain.AssetCustomFieldDefinitionRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.DepreciationMethod;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Category CRUD (FR-AST-03) plus per-category custom field definitions
 * (FR-AST-06). Delete is blocked (409) while any asset still references the
 * category, per AC-AST-03-X.
 */
@Service
public class AssetCategoryService {

    private final AssetCategoryRepository categoryRepository;
    private final AssetCustomFieldDefinitionRepository fieldDefinitionRepository;
    private final AssetRepository assetRepository;
    private final CurrentUserProvider currentUserProvider;

    public AssetCategoryService(AssetCategoryRepository categoryRepository,
                                 AssetCustomFieldDefinitionRepository fieldDefinitionRepository,
                                 AssetRepository assetRepository,
                                 CurrentUserProvider currentUserProvider) {
        this.categoryRepository = categoryRepository;
        this.fieldDefinitionRepository = fieldDefinitionRepository;
        this.assetRepository = assetRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<AssetCategory> list() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AssetCategory get(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> NotFoundException.of("AssetCategory", id));
    }

    @Transactional(readOnly = true)
    public List<AssetCustomFieldDefinition> fieldDefinitions(UUID categoryId) {
        return fieldDefinitionRepository.findByCategoryIdOrderByDisplayOrder(categoryId);
    }

    @Transactional
    public AssetCategory create(String name, String code, List<CustomFieldSpec> fields, Boolean requiresVehicleFields,
                                 DepreciationMethod defaultDepreciationMethod,
                                 Integer defaultUsefulLifeMonths, BigDecimal defaultSalvageValuePct) {
        UUID actor = currentUserProvider.current().id();

        AssetCategory category = new AssetCategory();
        category.setName(name);
        category.setCode(code);
        category.setActive(true);
        category.setRequiresVehicleFields(Boolean.TRUE.equals(requiresVehicleFields));
        category.setDefaultDepreciationMethod(defaultDepreciationMethod);
        category.setDefaultUsefulLifeMonths(defaultUsefulLifeMonths);
        category.setDefaultSalvageValuePct(defaultSalvageValuePct);
        category.setCreatedBy(actor);
        category = categoryRepository.save(category);

        saveFieldDefinitions(category, fields, actor);
        return category;
    }

    @Transactional
    public AssetCategory update(UUID id, String name, String code, Boolean active, List<CustomFieldSpec> fields,
                                 Boolean requiresVehicleFields,
                                 DepreciationMethod defaultDepreciationMethod,
                                 Integer defaultUsefulLifeMonths, BigDecimal defaultSalvageValuePct,
                                 long expectedVersion) {
        AssetCategory category = get(id);
        try {
            if (name != null) {
                category.setName(name);
            }
            if (code != null) {
                category.setCode(code);
            }
            if (active != null) {
                category.setActive(active);
            }
            if (requiresVehicleFields != null) {
                category.setRequiresVehicleFields(requiresVehicleFields);
            }
            if (defaultDepreciationMethod != null) {
                category.setDefaultDepreciationMethod(defaultDepreciationMethod);
            }
            if (defaultUsefulLifeMonths != null) {
                category.setDefaultUsefulLifeMonths(defaultUsefulLifeMonths);
            }
            if (defaultSalvageValuePct != null) {
                category.setDefaultSalvageValuePct(defaultSalvageValuePct);
            }
            category.setUpdatedBy(currentUserProvider.current().id());
            category = categoryRepository.saveAndFlush(category);
        } catch (OptimisticLockingFailureException e) {
            AssetCategory current = get(id);
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }

        if (fields != null) {
            fieldDefinitionRepository.deleteAll(fieldDefinitionRepository.findByCategoryIdOrderByDisplayOrder(id));
            saveFieldDefinitions(category, fields, currentUserProvider.current().id());
        }
        return category;
    }

    @Transactional
    public void delete(UUID id) {
        AssetCategory category = get(id);
        if (assetRepository.existsByCategoryId(id)) {
            // Was mislabeled ORG_NODE_HAS_DEPENDENTS (copy-paste from a template written
            // before EPIC-ORG existed) - that literal code is AC-ORG-01-X's, for actual
            // org-node deletion, not this resource.
            throw new ConflictException("CATEGORY_HAS_DEPENDENTS",
                    "Category '" + category.getName() + "' is still referenced by one or more assets and cannot be deleted.");
        }
        fieldDefinitionRepository.deleteAll(fieldDefinitionRepository.findByCategoryIdOrderByDisplayOrder(id));
        categoryRepository.delete(category);
    }

    private void saveFieldDefinitions(AssetCategory category, List<CustomFieldSpec> fields, UUID actor) {
        if (fields == null) {
            return;
        }
        int order = 0;
        for (CustomFieldSpec spec : fields) {
            AssetCustomFieldDefinition def = new AssetCustomFieldDefinition();
            def.setCategory(category);
            def.setFieldKey(spec.fieldKey());
            def.setLabel(spec.label());
            def.setDataType(spec.dataType());
            def.setRequired(spec.required());
            def.setEnumOptions(spec.enumOptions());
            def.setDisplayOrder(order++);
            def.setCreatedBy(actor);
            fieldDefinitionRepository.save(def);
        }
    }

    public record CustomFieldSpec(
            String fieldKey,
            String label,
            com.iams.asset.domain.CustomFieldDataType dataType,
            boolean required,
            List<String> enumOptions
    ) {
    }
}
