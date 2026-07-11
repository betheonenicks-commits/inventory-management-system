package com.iams.asset.domain;

import com.iams.common.domain.BaseEntity;
import com.iams.org.domain.OrgNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The core asset register entity (FR-AST-01 and the Phase-1 AST stories built
 * around it). Fields beyond Phase 1's scope (insurance, vehicle, depreciation,
 * full assignment workflow) are intentionally absent, not stubbed - they land
 * with their own stories/migrations later, per the walking-skeleton plan.
 */
@Getter
@Setter
@Entity
@Table(name = "asset")
public class Asset extends BaseEntity {

    @Column(name = "asset_number", nullable = false, unique = true, updatable = false)
    private String assetNumber;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private AssetCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private AssetStatusDef status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_node_id", nullable = false)
    private OrgNode orgNode;

    @Column(name = "assigned_to_person_id")
    private UUID assignedToPersonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_asset_id")
    private Asset parentAsset;

    @Column(name = "serial_number")
    private String serialNumber;

    private String manufacturer;

    private String model;

    private String description;

    @Column(name = "barcode_value", nullable = false, unique = true, updatable = false)
    private String barcodeValue;

    @Column(name = "qr_payload", nullable = false, unique = true, updatable = false)
    private String qrPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes", nullable = false)
    private Map<String, Object> customAttributes = new HashMap<>();

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "purchase_order_reference")
    private String purchaseOrderReference;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost")
    private BigDecimal purchaseCost;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;
}
