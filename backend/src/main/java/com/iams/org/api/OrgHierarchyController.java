package com.iams.org.api;

import com.iams.org.api.dto.OrgLevelRenameRequest;
import com.iams.org.api.dto.OrgLevelResponse;
import com.iams.org.api.dto.OrgNodeCreateRequest;
import com.iams.org.api.dto.OrgNodeResponse;
import com.iams.org.application.OrgHierarchyService;
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
 * US-ORG-01 (build the hierarchy), US-ORG-02 (relabel level names), US-ORG-06
 * (Room-level variants). Reads (levels, node list/get) are open to any
 * authenticated user, matching PersonController's convention - other forms
 * (asset registration, person creation) need the org-node picker regardless
 * of role. Mutations require org:write, matching PersonController's existing
 * ADMIN/SUPER_ADMIN scope for this epic.
 */
@RestController
@RequestMapping("/api/v1")
public class OrgHierarchyController {

    private final OrgHierarchyService hierarchyService;
    private final OrgHierarchyMapper mapper;

    public OrgHierarchyController(OrgHierarchyService hierarchyService, OrgHierarchyMapper mapper) {
        this.hierarchyService = hierarchyService;
        this.mapper = mapper;
    }

    @GetMapping("/org-levels")
    public List<OrgLevelResponse> listLevels() {
        return hierarchyService.listLevels().stream().map(mapper::toResponse).toList();
    }

    @PatchMapping("/org-levels/{id}")
    @PreAuthorize("@perm.has('org:write')")
    public OrgLevelResponse renameLevel(@PathVariable UUID id, @Valid @RequestBody OrgLevelRenameRequest request) {
        return mapper.toResponse(hierarchyService.renameLevel(id, request.name(), request.version()));
    }

    @GetMapping("/org-nodes")
    public List<OrgNodeResponse> list() {
        return hierarchyService.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/org-nodes/{id}")
    public OrgNodeResponse get(@PathVariable UUID id) {
        return mapper.toResponse(hierarchyService.get(id));
    }

    @PostMapping("/org-nodes")
    @PreAuthorize("@perm.has('org:write')")
    public ResponseEntity<OrgNodeResponse> create(@Valid @RequestBody OrgNodeCreateRequest request) {
        var node = hierarchyService.create(request.name(), request.code(), request.parentId(),
                request.levelId(), request.roomVariant());
        OrgNodeResponse response = mapper.toResponse(node);
        return ResponseEntity.created(URI.create("/api/v1/org-nodes/" + node.getId())).body(response);
    }

    @DeleteMapping("/org-nodes/{id}")
    @PreAuthorize("@perm.has('org:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        hierarchyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
