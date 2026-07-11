package com.iams.asset.api;

import com.iams.asset.api.dto.AssetCategoryRequest;
import com.iams.asset.api.dto.AssetCategoryResponse;
import com.iams.asset.api.mapper.AssetCategoryMapper;
import com.iams.asset.application.AssetCategoryService;
import com.iams.asset.domain.AssetCategory;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Category CRUD + per-category custom field definitions (FR-AST-03/06,
 * US-AST-03/06).
 */
@RestController
@RequestMapping("/api/v1/asset-categories")
public class AssetCategoryController {

    private final AssetCategoryService categoryService;
    private final AssetCategoryMapper mapper;

    public AssetCategoryController(AssetCategoryService categoryService, AssetCategoryMapper mapper) {
        this.categoryService = categoryService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<AssetCategoryResponse> list() {
        return categoryService.list().stream()
                .map(c -> mapper.toResponse(c, categoryService.fieldDefinitions(c.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public AssetCategoryResponse get(@PathVariable UUID id) {
        AssetCategory category = categoryService.get(id);
        return mapper.toResponse(category, categoryService.fieldDefinitions(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AssetCategoryResponse> create(@Valid @RequestBody AssetCategoryRequest request) {
        List<AssetCategoryService.CustomFieldSpec> fields = toSpecs(request);
        AssetCategory category = categoryService.create(request.name(), request.code(), fields);
        AssetCategoryResponse response = mapper.toResponse(category, categoryService.fieldDefinitions(category.getId()));
        return ResponseEntity.created(URI.create("/api/v1/asset-categories/" + category.getId())).body(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public AssetCategoryResponse update(@PathVariable UUID id, @Valid @RequestBody AssetCategoryRequest request) {
        long expectedVersion = request.version() != null ? request.version() : 0L;
        List<AssetCategoryService.CustomFieldSpec> fields = toSpecs(request);
        AssetCategory category = categoryService.update(id, request.name(), request.code(), request.active(), fields, expectedVersion);
        return mapper.toResponse(category, categoryService.fieldDefinitions(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private List<AssetCategoryService.CustomFieldSpec> toSpecs(AssetCategoryRequest request) {
        if (request.customFields() == null) {
            return null;
        }
        return request.customFields().stream()
                .map(f -> new AssetCategoryService.CustomFieldSpec(f.fieldKey(), f.label(), f.dataType(), f.required(), f.enumOptions()))
                .toList();
    }
}
