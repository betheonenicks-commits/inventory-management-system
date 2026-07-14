package com.iams.asset.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-asset depreciation override (FR-AST-16). Any null field here falls
 * back to the parent category's default for that field - DepreciationService
 * is where that resolution happens, not here.
 */
@Getter
@Setter
@Entity
@Table(name = "asset_depreciation_override")
public class AssetDepreciationOverride extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, unique = true, updatable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    private DepreciationMethod method;

    @Column(name = "useful_life_months")
    private Integer usefulLifeMonths;

    @Column(name = "salvage_value_pct")
    private BigDecimal salvageValuePct;

    @Column(name = "depreciation_start_date")
    private LocalDate depreciationStartDate;
}
