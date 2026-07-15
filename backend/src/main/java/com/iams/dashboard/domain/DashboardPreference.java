package com.iams.dashboard.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-DSH-06: one row per user holding their chosen KPI tile subset. "No row
 * yet" is the default-set state (all available tiles render); an existing row
 * with an empty list is a deliberate, saved choice to show nothing - the two
 * are distinguishable on purpose. Tiles are stored as enum names in jsonb;
 * {@link com.iams.dashboard.application.DashboardPreferenceService} converts
 * to/from {@link DashboardTile} and is where validation lives.
 */
@Getter
@Setter
@Entity
@Table(name = "dashboard_preference")
public class DashboardPreference extends BaseEntity {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> tiles = new ArrayList<>();
}
