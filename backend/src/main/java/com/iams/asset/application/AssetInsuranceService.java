package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetInsuranceDetail;
import com.iams.asset.domain.AssetInsuranceDetailRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurance policy tracking (FR-AST-14, US-AST-14). Upsert-by-asset-id, same
 * shape as AssetStatusService's optimistic-lock handling. Unlike warranty
 * (plain columns on Asset), this is its own table per the Data Dictionary,
 * so it gets its own version rather than riding the asset's.
 */
@Service
public class AssetInsuranceService {

    private final AssetRepository assetRepository;
    private final AssetInsuranceDetailRepository insuranceRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final CurrentUserProvider currentUserProvider;

    public AssetInsuranceService(AssetRepository assetRepository,
                                  AssetInsuranceDetailRepository insuranceRepository,
                                  AssetHistoryRecorder historyRecorder,
                                  CurrentUserProvider currentUserProvider) {
        this.assetRepository = assetRepository;
        this.insuranceRepository = insuranceRepository;
        this.historyRecorder = historyRecorder;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Optional<AssetInsuranceDetail> get(UUID assetId) {
        return insuranceRepository.findByAssetId(assetId);
    }

    @Transactional
    public AssetInsuranceDetail upsert(UUID assetId, String insurerName, String policyNumber,
                                        BigDecimal coverageAmount, String coverageCurrency,
                                        LocalDate policyStartDate, LocalDate policyExpiryDate,
                                        Long expectedVersion) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
        validateDates(policyStartDate, policyExpiryDate);
        validateCoverageAmount(coverageAmount);

        Optional<AssetInsuranceDetail> existing = insuranceRepository.findByAssetId(assetId);
        AssetInsuranceDetail detail;
        UUID actor = currentUserProvider.current().id();

        if (existing.isPresent()) {
            detail = existing.get();
            long expected = expectedVersion != null ? expectedVersion : 0L;
            if (detail.getVersion() != expected) {
                throw new OptimisticLockConflictException(expected, detail.getVersion(), detail);
            }
            detail.setUpdatedBy(actor);
        } else {
            detail = new AssetInsuranceDetail();
            detail.setAsset(asset);
            detail.setCreatedBy(actor);
        }

        detail.setInsurerName(insurerName);
        detail.setPolicyNumber(policyNumber);
        detail.setCoverageAmount(coverageAmount);
        detail.setCoverageCurrency(coverageCurrency);
        detail.setPolicyStartDate(policyStartDate);
        detail.setPolicyExpiryDate(policyExpiryDate);

        try {
            detail = insuranceRepository.saveAndFlush(detail);
        } catch (OptimisticLockingFailureException e) {
            AssetInsuranceDetail current = insuranceRepository.findByAssetId(assetId)
                    .orElseThrow(() -> NotFoundException.of("AssetInsuranceDetail", assetId));
            throw new OptimisticLockConflictException(expectedVersion != null ? expectedVersion : 0L, current.getVersion(), current);
        }

        historyRecorder.record(asset, AssetHistoryEventType.FIELD_UPDATE, "insurance", null,
                policyNumber != null ? policyNumber : "updated");
        return detail;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw ValidationFailedException.singleField("policyExpiryDate", "Must not be before policyStartDate");
        }
    }

    private void validateCoverageAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw ValidationFailedException.singleField("coverageAmount", "Must not be negative");
        }
    }
}
