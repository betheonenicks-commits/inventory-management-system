package com.iams.inventory.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * US-INV-01: consumable/bulk stock tracked by quantity, never as N individual
 * {@code asset} rows - this entity and {@link InventoryStockBalance} are the
 * entire model; nothing here ever creates an {@code Asset} record, and
 * nothing in {@code com.iams.asset} creates one of these. A serialized unit
 * pulled from bulk stock into an individually tracked asset (the story's own
 * example) is a manual, separate action in each module, not an automatic
 * conversion - no AC asks for one, and inventing it would be scope no story asked for.
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_item")
public class InventoryItem extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    /** US-INV-11: displayed alongside every quantity everywhere it appears - no cross-unit conversion exists. */
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false)
    private UnitOfMeasure unitOfMeasure;

    /** US-INV-04: null means no reorder alerting is configured for this item. */
    @Column(name = "reorder_level", precision = 14, scale = 3)
    private BigDecimal reorderLevel;

    /** US-INV-06: WEIGHTED_AVERAGE by default - the only method with defensible, non-last-price valuation. */
    @Enumerated(EnumType.STRING)
    @Column(name = "costing_method", nullable = false)
    private CostingMethod costingMethod = CostingMethod.WEIGHTED_AVERAGE;

    @Column(nullable = false)
    private boolean active = true;
}
