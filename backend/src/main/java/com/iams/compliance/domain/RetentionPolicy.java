package com.iams.compliance.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** US-CMP-01: one row per entity type - "how long, then what." */
@Getter
@Setter
@Entity
@Table(name = "retention_policy")
public class RetentionPolicy extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, unique = true)
    private RetentionEntityType entityType;

    @Column(name = "retention_period_days", nullable = false)
    private int retentionPeriodDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_action", nullable = false)
    private RetentionExpiryAction expiryAction;
}
