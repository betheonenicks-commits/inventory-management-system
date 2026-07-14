package com.iams.asset.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Configurable asset category (FR-AST-03). Custom-field schema lives in the
 * separate AssetCustomFieldDefinition table, not a JSON-Schema-in-column blob.
 */
@Getter
@Setter
@Entity
@Table(name = "asset_category")
public class AssetCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private AssetCategory parentCategory;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "requires_vehicle_fields", nullable = false)
    private boolean requiresVehicleFields = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_depreciation_method")
    private DepreciationMethod defaultDepreciationMethod;

    @Column(name = "default_useful_life_months")
    private Integer defaultUsefulLifeMonths;

    @Column(name = "default_salvage_value_pct")
    private BigDecimal defaultSalvageValuePct;
}
