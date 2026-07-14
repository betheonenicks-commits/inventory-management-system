package com.iams.asset.api;

import com.iams.asset.api.dto.VehicleDetailRequest;
import com.iams.asset.api.dto.VehicleDetailResponse;
import com.iams.asset.application.AssetVehicleService;
import com.iams.asset.domain.VehicleDetail;
import com.iams.common.exception.NotFoundException;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vehicle-attribute endpoints (FR-AST-15). Split out the same way
 * insurance/status/label/children/assignment are.
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetVehicleController {

    private final AssetVehicleService vehicleService;

    public AssetVehicleController(AssetVehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/{id}/vehicle")
    public VehicleDetailResponse get(@PathVariable UUID id) {
        VehicleDetail detail = vehicleService.get(id).orElseThrow(() -> NotFoundException.of("VehicleDetail", id));
        return toResponse(detail);
    }

    @PutMapping("/{id}/vehicle")
    @PreAuthorize("@perm.has('assets:write')")
    public VehicleDetailResponse upsert(@PathVariable UUID id, @RequestBody VehicleDetailRequest request) {
        VehicleDetail detail = vehicleService.upsert(id, request.vin(), request.registrationNumber(),
                request.odometerReading(), request.odometerUnit(), request.registrationExpiryDate(),
                request.insuranceExpiryDate(), request.version());
        return toResponse(detail);
    }

    private VehicleDetailResponse toResponse(VehicleDetail detail) {
        return new VehicleDetailResponse(
                detail.getId(),
                detail.getVersion(),
                detail.getAsset().getId(),
                detail.getVin(),
                detail.getRegistrationNumber(),
                detail.getOdometerReading(),
                detail.getOdometerUnit(),
                detail.getRegistrationExpiryDate(),
                detail.getInsuranceExpiryDate()
        );
    }
}
