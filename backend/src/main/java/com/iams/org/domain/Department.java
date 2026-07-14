package com.iams.org.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * FR-ORG-03: departments/cost centers as their own dimension, independent of
 * the physical org_node hierarchy - a budget-owning unit that spans multiple
 * buildings, or has no physical footprint at all, is still trackable. Not
 * yet referenced by Asset or Person (see DEVELOPMENT_LOG.md 2026-07-13) -
 * this is the standalone dimension itself, deliberately shipped before that
 * wiring rather than blocked on it.
 */
@Getter
@Setter
@Entity
@Table(name = "department")
public class Department extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "cost_center_code", nullable = false, unique = true)
    private String costCenterCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
