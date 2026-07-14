package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.VehicleDetail;
import com.iams.asset.domain.VehicleDetailRepository;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vehicle attributes (FR-AST-15, US-AST-15). Same upsert-by-asset-id shape as
 * AssetInsuranceService - copied, not re-derived, since both are 1:1
 * category-gated detail tables with identical optimistic-lock needs.
 */
@Service
public class AssetVehicleService {

    private final AssetRepository assetRepository;
    private final VehicleDetailRepository vehicleRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final CurrentUserProvider currentUserProvider;

    public AssetVehicleService(AssetRepository assetRepository,
                                VehicleDetailRepository vehicleRepository,
                                AssetHistoryRecorder historyRecorder,
                                CurrentUserProvider currentUserProvider) {
        this.assetRepository = assetRepository;
        this.vehicleRepository = vehicleRepository;
        this.historyRecorder = historyRecorder;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Optional<VehicleDetail> get(UUID assetId) {
        return vehicleRepository.findByAssetId(assetId);
    }

    @Transactional
    public VehicleDetail upsert(UUID assetId, String vin, String registrationNumber, Integer odometerReading,
                                 String odometerUnit, LocalDate registrationExpiryDate, LocalDate insuranceExpiryDate,
                                 Long expectedVersion) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
        if (odometerReading != null && odometerReading < 0) {
            throw ValidationFailedException.singleField("odometerReading", "Must not be negative");
        }

        Optional<VehicleDetail> existing = vehicleRepository.findByAssetId(assetId);
        VehicleDetail detail;
        UUID actor = currentUserProvider.current().id();

        if (existing.isPresent()) {
            detail = existing.get();
            long expected = expectedVersion != null ? expectedVersion : 0L;
            if (detail.getVersion() != expected) {
                throw new OptimisticLockConflictException(expected, detail.getVersion(), detail);
            }
            detail.setUpdatedBy(actor);
        } else {
            detail = new VehicleDetail();
            detail.setAsset(asset);
            detail.setCreatedBy(actor);
        }

        detail.setVin(vin);
        detail.setRegistrationNumber(registrationNumber);
        detail.setOdometerReading(odometerReading);
        detail.setOdometerUnit(odometerUnit != null ? odometerUnit : "MI");
        detail.setRegistrationExpiryDate(registrationExpiryDate);
        detail.setInsuranceExpiryDate(insuranceExpiryDate);

        try {
            detail = vehicleRepository.saveAndFlush(detail);
        } catch (OptimisticLockingFailureException e) {
            VehicleDetail current = vehicleRepository.findByAssetId(assetId)
                    .orElseThrow(() -> NotFoundException.of("VehicleDetail", assetId));
            throw new OptimisticLockConflictException(expectedVersion != null ? expectedVersion : 0L, current.getVersion(), current);
        }

        historyRecorder.record(asset, AssetHistoryEventType.FIELD_UPDATE, "vehicleDetail", null,
                registrationNumber != null ? registrationNumber : "updated");
        return detail;
    }
}
