package com.iams.asset.api;

import com.iams.asset.api.dto.AssetStatusResponse;
import com.iams.asset.application.AssetStatusService;
import com.iams.asset.domain.AssetStatusDef;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The configurable status list (FR-AST-07). Read-only for Phase 1 - admin
 * CRUD over this list is a follow-up story.
 */
@RestController
@RequestMapping("/api/v1/asset-statuses")
public class AssetStatusController {

    private final AssetStatusService statusService;

    public AssetStatusController(AssetStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping
    public List<AssetStatusResponse> list() {
        return statusService.availableStatuses().stream()
                .sorted(Comparator.comparingInt(AssetStatusDef::getSortOrder))
                .map(s -> new AssetStatusResponse(s.getId(), s.getCode(), s.getLabel(), s.isTerminal(), s.getSortOrder()))
                .toList();
    }
}
