package com.iams.asset.api;

import com.iams.asset.api.dto.DepreciationOverrideRequest;
import com.iams.asset.api.dto.DepreciationOverrideResponse;
import com.iams.asset.api.dto.DepreciationResponse;
import com.iams.asset.application.DepreciationResult;
import com.iams.asset.application.DepreciationService;
import com.iams.asset.domain.AssetDepreciationOverride;
import com.iams.common.exception.NotFoundException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Net book value computation + per-asset override endpoints (FR-AST-16).
 * Split out the same way the other detail panels are.
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetDepreciationController {

    private final DepreciationService depreciationService;

    public AssetDepreciationController(DepreciationService depreciationService) {
        this.depreciationService = depreciationService;
    }

    @GetMapping("/{id}/depreciation")
    public DepreciationResponse compute(@PathVariable UUID id,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        DepreciationResult result = depreciationService.compute(id, asOf);
        return new DepreciationResponse(
                result.status().name(),
                result.method() != null ? result.method().name() : null,
                result.usefulLifeMonths(),
                result.salvageValue(),
                result.monthlyDepreciation(),
                result.accumulatedDepreciation(),
                result.netBookValue(),
                result.asOf()
        );
    }

    @GetMapping("/{id}/depreciation-override")
    public DepreciationOverrideResponse getOverride(@PathVariable UUID id) {
        AssetDepreciationOverride override = depreciationService.getOverride(id)
                .orElseThrow(() -> NotFoundException.of("AssetDepreciationOverride", id));
        return toOverrideResponse(override);
    }

    @PutMapping("/{id}/depreciation-override")
    @PreAuthorize("@perm.has('assets:write')")
    public DepreciationOverrideResponse upsertOverride(@PathVariable UUID id, @RequestBody DepreciationOverrideRequest request) {
        AssetDepreciationOverride override = depreciationService.upsertOverride(id, request.method(), request.usefulLifeMonths(),
                request.salvageValuePct(), request.depreciationStartDate(), request.version());
        return toOverrideResponse(override);
    }

    private DepreciationOverrideResponse toOverrideResponse(AssetDepreciationOverride override) {
        return new DepreciationOverrideResponse(
                override.getId(),
                override.getVersion(),
                override.getAsset().getId(),
                override.getMethod(),
                override.getUsefulLifeMonths(),
                override.getSalvageValuePct(),
                override.getDepreciationStartDate()
        );
    }
}
