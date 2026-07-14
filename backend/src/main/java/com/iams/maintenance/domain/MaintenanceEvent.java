package com.iams.maintenance.domain;

import com.iams.asset.domain.Asset;
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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-LIF-07 (a completion recorded against a schedule) and US-LIF-08 (an
 * unscheduled corrective event, no schedule). schedule is null for CORRECTIVE
 * by construction (MaintenanceEventService never sets it for that type) -
 * that's what "distinct from Preventive" (AC-LIF-08-H) means structurally,
 * not just a label.
 */
@Getter
@Setter
@Entity
@Table(name = "maintenance_event")
public class MaintenanceEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private MaintenanceSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 15)
    private MaintenanceType maintenanceType;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(length = 1000)
    private String notes;

    @Column(name = "cost")
    private BigDecimal cost;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;
}
