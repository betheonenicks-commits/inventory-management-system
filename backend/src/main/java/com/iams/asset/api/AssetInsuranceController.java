package com.iams.asset.api;

import com.iams.asset.api.dto.AssetInsuranceRequest;
import com.iams.asset.api.dto.AssetInsuranceResponse;
import com.iams.asset.application.AssetInsuranceService;
import com.iams.asset.domain.AssetInsuranceDetail;
import com.iams.common.exception.NotFoundException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Insurance policy tracking endpoints (FR-AST-14). Split out from
 * AssetController the same way status/label/children/assignment are.
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetInsuranceController {

    private final AssetInsuranceService insuranceService;

    public AssetInsuranceController(AssetInsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @GetMapping("/{id}/insurance")
    public AssetInsuranceResponse get(@PathVariable UUID id) {
        AssetInsuranceDetail detail = insuranceService.get(id)
                .orElseThrow(() -> NotFoundException.of("AssetInsuranceDetail", id));
        return toResponse(detail);
    }

    @PutMapping("/{id}/insurance")
    @PreAuthorize("@perm.has('assets:write')")
    public AssetInsuranceResponse upsert(@PathVariable UUID id, @RequestBody AssetInsuranceRequest request) {
        AssetInsuranceDetail detail = insuranceService.upsert(id, request.insurerName(), request.policyNumber(),
                request.coverageAmount(), request.coverageCurrency(), request.policyStartDate(),
                request.policyExpiryDate(), request.version());
        return toResponse(detail);
    }

    private AssetInsuranceResponse toResponse(AssetInsuranceDetail detail) {
        boolean expired = detail.getPolicyExpiryDate() != null && detail.getPolicyExpiryDate().isBefore(LocalDate.now());
        return new AssetInsuranceResponse(
                detail.getId(),
                detail.getVersion(),
                detail.getAsset().getId(),
                detail.getInsurerName(),
                detail.getPolicyNumber(),
                detail.getCoverageAmount(),
                detail.getCoverageCurrency(),
                detail.getPolicyStartDate(),
                detail.getPolicyExpiryDate(),
                expired
        );
    }
}
