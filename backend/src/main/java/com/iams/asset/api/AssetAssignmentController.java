package com.iams.asset.api;

import com.iams.asset.api.dto.AssetAssignmentRequest;
import com.iams.asset.api.dto.AssetResponse;
import com.iams.asset.api.mapper.AssetMapper;
import com.iams.asset.application.AssetAssignmentService;
import com.iams.asset.domain.Asset;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Custodian assignment endpoints (FR-LIF-04). Kept separate from
 * AssetController the same way status/label/children are.
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetAssignmentController {

    private final AssetAssignmentService assignmentService;
    private final AssetMapper mapper;

    public AssetAssignmentController(AssetAssignmentService assignmentService, AssetMapper mapper) {
        this.assignmentService = assignmentService;
        this.mapper = mapper;
    }

    @PostMapping("/{id}/assignment")
    @PreAuthorize("@perm.has('assets:write')")
    public AssetResponse assign(@PathVariable UUID id, @Valid @RequestBody AssetAssignmentRequest request) {
        Asset asset = assignmentService.assign(id, request.personId(), request.version());
        return mapper.toResponse(asset);
    }

    @DeleteMapping("/{id}/assignment")
    @PreAuthorize("@perm.has('assets:write')")
    public AssetResponse unassign(@PathVariable UUID id, @RequestParam long version) {
        Asset asset = assignmentService.unassign(id, version);
        return mapper.toResponse(asset);
    }
}
