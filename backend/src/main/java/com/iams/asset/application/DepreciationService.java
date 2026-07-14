package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetDepreciationOverride;
import com.iams.asset.domain.AssetDepreciationOverrideRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.DepreciationMethod;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Net book value computation (FR-AST-16, US-AST-16). Computed on demand from
 * stored parameters - no persisted per-period entry table, since nothing
 * consumes historical entries yet (no report epic reads them). Only
 * STRAIGHT_LINE is implemented; DECLINING_BALANCE is rejected with a clear
 * error at computation time rather than producing silently wrong output.
 */
@Service
public class DepreciationService {

    private static final int MONEY_SCALE = 2;

    private final AssetRepository assetRepository;
    private final AssetDepreciationOverrideRepository overrideRepository;
    private final CurrentUserProvider currentUserProvider;

    public DepreciationService(AssetRepository assetRepository,
                                AssetDepreciationOverrideRepository overrideRepository,
                                CurrentUserProvider currentUserProvider) {
        this.assetRepository = assetRepository;
        this.overrideRepository = overrideRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Optional<AssetDepreciationOverride> getOverride(UUID assetId) {
        return overrideRepository.findByAssetId(assetId);
    }

    @Transactional(readOnly = true)
    public DepreciationResult compute(UUID assetId, LocalDate asOf) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
        LocalDate effectiveAsOf = asOf != null ? asOf : LocalDate.now();

        Optional<AssetDepreciationOverride> override = overrideRepository.findByAssetId(assetId);

        DepreciationMethod method = resolve(override.map(AssetDepreciationOverride::getMethod).orElse(null),
                asset.getCategory().getDefaultDepreciationMethod());
        Integer usefulLifeMonths = resolve(override.map(AssetDepreciationOverride::getUsefulLifeMonths).orElse(null),
                asset.getCategory().getDefaultUsefulLifeMonths());
        BigDecimal salvagePct = resolve(override.map(AssetDepreciationOverride::getSalvageValuePct).orElse(null),
                asset.getCategory().getDefaultSalvageValuePct());
        LocalDate inServiceDate = override.map(AssetDepreciationOverride::getDepreciationStartDate)
                .orElse(asset.getPurchaseDate());

        if (method == null || usefulLifeMonths == null || asset.getPurchaseCost() == null || inServiceDate == null) {
            return DepreciationResult.notDepreciated(effectiveAsOf);
        }
        if (method == DepreciationMethod.DECLINING_BALANCE) {
            throw ValidationFailedException.singleField("method", "DECLINING_BALANCE is not yet supported");
        }

        BigDecimal purchaseCost = asset.getPurchaseCost();
        BigDecimal salvageFraction = (salvagePct != null ? salvagePct : BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal salvageValue = purchaseCost.multiply(salvageFraction).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal depreciableBase = purchaseCost.subtract(salvageValue);

        BigDecimal monthlyDepreciation = usefulLifeMonths > 0
                ? depreciableBase.divide(BigDecimal.valueOf(usefulLifeMonths), MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long monthsElapsed = monthsElapsedFullMonthConvention(inServiceDate, effectiveAsOf);
        long cappedMonths = Math.max(0, Math.min(monthsElapsed, usefulLifeMonths));

        BigDecimal accumulated = monthlyDepreciation.multiply(BigDecimal.valueOf(cappedMonths));
        if (accumulated.compareTo(depreciableBase) > 0) {
            accumulated = depreciableBase;
        }
        BigDecimal netBookValue = purchaseCost.subtract(accumulated);

        return new DepreciationResult(DepreciationResult.Status.COMPUTED, method, usefulLifeMonths, salvageValue,
                monthlyDepreciation, accumulated, netBookValue, effectiveAsOf);
    }

    @Transactional
    public AssetDepreciationOverride upsertOverride(UUID assetId, DepreciationMethod method, Integer usefulLifeMonths,
                                                      BigDecimal salvageValuePct, LocalDate depreciationStartDate,
                                                      Long expectedVersion) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
        if (salvageValuePct != null && (salvageValuePct.compareTo(BigDecimal.ZERO) < 0 || salvageValuePct.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw ValidationFailedException.singleField("salvageValuePct", "Must be between 0 and 100");
        }
        if (usefulLifeMonths != null && usefulLifeMonths <= 0) {
            throw ValidationFailedException.singleField("usefulLifeMonths", "Must be positive");
        }

        Optional<AssetDepreciationOverride> existing = overrideRepository.findByAssetId(assetId);
        AssetDepreciationOverride override;
        UUID actor = currentUserProvider.current().id();

        if (existing.isPresent()) {
            override = existing.get();
            long expected = expectedVersion != null ? expectedVersion : 0L;
            if (override.getVersion() != expected) {
                throw new OptimisticLockConflictException(expected, override.getVersion(), override);
            }
            override.setUpdatedBy(actor);
        } else {
            override = new AssetDepreciationOverride();
            override.setAsset(asset);
            override.setCreatedBy(actor);
        }

        override.setMethod(method);
        override.setUsefulLifeMonths(usefulLifeMonths);
        override.setSalvageValuePct(salvageValuePct);
        override.setDepreciationStartDate(depreciationStartDate);

        try {
            return overrideRepository.saveAndFlush(override);
        } catch (OptimisticLockingFailureException e) {
            AssetDepreciationOverride current = overrideRepository.findByAssetId(assetId)
                    .orElseThrow(() -> NotFoundException.of("AssetDepreciationOverride", assetId));
            throw new OptimisticLockConflictException(expectedVersion != null ? expectedVersion : 0L, current.getVersion(), current);
        }
    }

    private <T> T resolve(T override, T categoryDefault) {
        return override != null ? override : categoryDefault;
    }

    private long monthsElapsedFullMonthConvention(LocalDate inServiceDate, LocalDate asOf) {
        if (asOf.isBefore(inServiceDate)) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(inServiceDate, asOf) + 1;
    }
}
