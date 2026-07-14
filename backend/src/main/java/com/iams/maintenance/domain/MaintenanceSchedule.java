package com.iams.maintenance.domain;

import com.iams.asset.domain.Asset;
import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * US-LIF-07: a recurring preventive-maintenance cadence for a
 * maintenance-critical asset (e.g. "every 6 months"). nextDueDate advances by
 * intervalMonths from its own prior value each time a completion is recorded
 * (MaintenanceEventService.recordPreventive), not from today's date - keeps
 * the cadence itself from drifting later every time completion runs a little
 * late or early.
 */
@Getter
@Setter
@Entity
@Table(name = "maintenance_schedule")
public class MaintenanceSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "interval_months", nullable = false)
    private int intervalMonths;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
