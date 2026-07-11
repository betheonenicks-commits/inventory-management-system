package com.iams.asset.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * The configurable status list (FR-AST-07). Admin CRUD for this table is a
 * follow-up story; Phase 1 only reads the seven seeded rows.
 */
@Getter
@Setter
@Entity
@Table(name = "asset_status_def")
public class AssetStatusDef extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(name = "is_terminal", nullable = false)
    private boolean terminal;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
