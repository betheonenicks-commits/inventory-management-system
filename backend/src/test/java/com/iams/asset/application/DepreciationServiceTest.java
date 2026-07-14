package com.iams.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetDepreciationOverride;
import com.iams.asset.domain.AssetDepreciationOverrideRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.DepreciationMethod;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepreciationServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetDepreciationOverrideRepository overrideRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private DepreciationService service;

    @BeforeEach
    void setUp() {
        service = new DepreciationService(assetRepository, overrideRepository, currentUserProvider);
    }

    private Asset asset(BigDecimal cost, LocalDate purchaseDate, DepreciationMethod method,
                         Integer usefulLifeMonths, BigDecimal salvagePct) {
        AssetCategory category = new AssetCategory();
        category.setDefaultDepreciationMethod(method);
        category.setDefaultUsefulLifeMonths(usefulLifeMonths);
        category.setDefaultSalvageValuePct(salvagePct);

        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setCategory(category);
        asset.setPurchaseCost(cost);
        asset.setPurchaseDate(purchaseDate);
        return asset;
    }

    @Test
    void computesMidScheduleStraightLine_zeroSalvage() {
        Asset asset = asset(BigDecimal.valueOf(24000), LocalDate.of(2024, 1, 1),
                DepreciationMethod.STRAIGHT_LINE, 24, BigDecimal.ZERO);
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(overrideRepository.findByAssetId(asset.getId())).thenReturn(Optional.empty());

        DepreciationResult result = service.compute(asset.getId(), LocalDate.of(2024, 12, 1));

        assertThat(result.status()).isEqualTo(DepreciationResult.Status.COMPUTED);
        assertThat(result.monthlyDepreciation()).isEqualByComparingTo("1000.00");
        assertThat(result.accumulatedDepreciation()).isEqualByComparingTo("12000.00");
        assertThat(result.netBookValue()).isEqualByComparingTo("12000.00");
    }

    @Test
    void fullyDepreciated_capsAtDepreciableBase() {
        Asset asset = asset(BigDecimal.valueOf(24000), LocalDate.of(2024, 1, 1),
                DepreciationMethod.STRAIGHT_LINE, 24, BigDecimal.ZERO);
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(overrideRepository.findByAssetId(asset.getId())).thenReturn(Optional.empty());

        DepreciationResult result = service.compute(asset.getId(), LocalDate.of(2026, 7, 1));

        assertThat(result.accumulatedDepreciation()).isEqualByComparingTo("24000.00");
        assertThat(result.netBookValue()).isEqualByComparingTo("0.00");
    }

    @Test
    void appliesSalvageValue() {
        Asset asset = asset(BigDecimal.valueOf(10000), LocalDate.of(2024, 1, 1),
                DepreciationMethod.STRAIGHT_LINE, 10, BigDecimal.TEN);
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(overrideRepository.findByAssetId(asset.getId())).thenReturn(Optional.empty());

        DepreciationResult result = service.compute(asset.getId(), LocalDate.of(2024, 1, 1));

        assertThat(result.salvageValue()).isEqualByComparingTo("1000.00");
        assertThat(result.monthlyDepreciation()).isEqualByComparingTo("900.00");
        assertThat(result.netBookValue()).isEqualByComparingTo("9100.00");
    }

    @Test
    void perAssetOverride_resolvesFieldByField() {
        Asset asset = asset(BigDecimal.valueOf(12000), LocalDate.of(2024, 1, 1),
                DepreciationMethod.STRAIGHT_LINE, 24, BigDecimal.ZERO);
        AssetDepreciationOverride override = new AssetDepreciationOverride();
        override.setUsefulLifeMonths(12); // only useful life overridden; method/salvage fall back to category
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(overrideRepository.findByAssetId(asset.getId())).thenReturn(Optional.of(override));

        DepreciationResult result = service.compute(asset.getId(), LocalDate.of(2024, 1, 1));

        assertThat(result.usefulLifeMonths()).isEqualTo(12);
        assertThat(result.monthlyDepreciation()).isEqualByComparingTo("1000.00");
        assertThat(result.netBookValue()).isEqualByComparingTo("11000.00");
    }

    @Test
    void notDepreciated_whenNothingConfigured() {
        Asset asset = asset(BigDecimal.valueOf(12000), LocalDate.of(2024, 1, 1), null, null, null);
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(overrideRepository.findByAssetId(asset.getId())).thenReturn(Optional.empty());

        DepreciationResult result = service.compute(asset.getId(), LocalDate.of(2024, 1, 1));

        assertThat(result.status()).isEqualTo(DepreciationResult.Status.NOT_DEPRECIATED);
        assertThat(result.netBookValue()).isNull();
    }

    @Test
    void decliningBalance_rejectedAtComputationTime() {
        Asset asset = asset(BigDecimal.valueOf(12000), LocalDate.of(2024, 1, 1),
                DepreciationMethod.DECLINING_BALANCE, 24, BigDecimal.ZERO);
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(overrideRepository.findByAssetId(asset.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.compute(asset.getId(), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(ValidationFailedException.class);
    }
}
