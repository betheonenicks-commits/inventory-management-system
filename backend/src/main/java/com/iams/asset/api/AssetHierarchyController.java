package com.iams.asset.api;

import com.iams.asset.api.dto.AssetChildLinkRequest;
import com.iams.asset.api.dto.AssetResponse;
import com.iams.asset.api.mapper.AssetMapper;
import com.iams.asset.application.AssetHierarchyService;
import com.iams.asset.domain.Asset;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parent-child (component) asset linking endpoints (FR-AST-04, US-AST-04).
 * Kept separate from AssetController the same way label/category/status are -
 * a cohesive sub-resource with its own service.
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetHierarchyController {

    private final AssetHierarchyService hierarchyService;
    private final AssetMapper mapper;

    public AssetHierarchyController(AssetHierarchyService hierarchyService, AssetMapper mapper) {
        this.hierarchyService = hierarchyService;
        this.mapper = mapper;
    }

    @GetMapping("/{id}/children")
    public List<AssetResponse> children(@PathVariable UUID id) {
        return hierarchyService.listChildren(id).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{id}/children")
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<AssetResponse> linkChild(@PathVariable UUID id, @Valid @RequestBody AssetChildLinkRequest request) {
        Asset child = hierarchyService.linkChild(id, request.childAssetId());
        return ResponseEntity.created(URI.create("/api/v1/assets/" + child.getId())).body(mapper.toResponse(child));
    }

    @DeleteMapping("/{id}/children/{childId}")
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<Void> unlinkChild(@PathVariable UUID id, @PathVariable UUID childId) {
        hierarchyService.unlinkChild(id, childId);
        return ResponseEntity.noContent().build();
    }
}
