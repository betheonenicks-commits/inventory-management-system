package com.iams.asset.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Vehicle-specific attributes (FR-AST-15). Only meaningful when the parent
 * asset's category has requiresVehicleFields set - the frontend hides this
 * panel otherwise, but nothing here enforces that at the data layer, since a
 * category can be reconfigured after assets already exist in it.
 */
@Getter
@Setter
@Entity
@Table(name = "vehicle_detail")
public class VehicleDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, unique = true, updatable = false)
    private Asset asset;

    private String vin;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "odometer_reading")
    private Integer odometerReading;

    @Column(name = "odometer_unit", nullable = false)
    private String odometerUnit = "MI";

    @Column(name = "registration_expiry_date")
    private LocalDate registrationExpiryDate;

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;
}
