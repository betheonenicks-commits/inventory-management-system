package com.iams.asset.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Current insurance policy for an asset (FR-AST-14). One row per asset, not
 * append-only - unlike AssetHistoryEvent, a policy is genuinely edited in
 * place (renewed, corrected); the parent asset's history log records that a
 * change happened, this table is the current state.
 */
@Getter
@Setter
@Entity
@Table(name = "asset_insurance_detail")
public class AssetInsuranceDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, unique = true, updatable = false)
    private Asset asset;

    @Column(name = "insurer_name")
    private String insurerName;

    @Column(name = "policy_number")
    private String policyNumber;

    @Column(name = "coverage_amount")
    private BigDecimal coverageAmount;

    // @JdbcTypeCode(SqlTypes.CHAR) matches V12__create_asset_insurance_detail.sql's CHAR(3)
    // exactly (a fixed-width ISO 4217 code) - a bare String @Column defaults to VARCHAR,
    // and ddl-auto:validate compares JDBC type *categories*, not columnDefinition strings,
    // so columnDefinition alone does not satisfy the validator; the type code must change.
    // Fixed here, not in the migration: V12 already shipped/ran, and this codebase's own
    // convention (V8, V10, V11 as incremental ALTERs) is to never edit an applied migration.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "coverage_currency", length = 3)
    private String coverageCurrency;

    @Column(name = "policy_start_date")
    private LocalDate policyStartDate;

    @Column(name = "policy_expiry_date")
    private LocalDate policyExpiryDate;
}
