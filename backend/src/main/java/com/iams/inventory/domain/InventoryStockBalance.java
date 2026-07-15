package com.iams.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-INV-01/03/09: the current, mutable quantity for one distinct
 * item/warehouse/sub-location/lot combination - deliberately not
 * {@code BaseEntity}: this is a live running total maintained alongside every
 * {@link InventoryTransaction}, not an audited "who created/changed this"
 * record in its own right (the transaction ledger is that record). Only
 * {@code @Version} is carried, to guard against a lost-update race between two
 * concurrent stock movements against the same row. subLocation/lotNumber
 * default to {@code ""} (never null) so the DB's natural-key UNIQUE constraint
 * (see V36) can be a plain unique index.
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_stock_balance")
public class InventoryStockBalance {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false, updatable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @Column(name = "sub_location", nullable = false, updatable = false)
    private String subLocation = "";

    @Column(name = "lot_number", nullable = false, updatable = false)
    private String lotNumber = "";

    /** US-INV-09: only ever set alongside a real lotNumber. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "quantity_on_hand", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    /** US-INV-06: recalculated on each STOCK_IN receipt when the item's costing method is WEIGHTED_AVERAGE. */
    @Column(name = "average_unit_cost", precision = 14, scale = 4)
    private BigDecimal averageUnitCost;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
