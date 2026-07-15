package com.iams.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-INV-06 (AC-INV-06-H): "the change itself is recorded" when an item's
 * costing method is switched - append-only, no version/updated_* columns,
 * same immutability discipline as {@code AuditFindingCorrection}/
 * {@code PurchaseOrderLineEvent}. Distinct from {@code InventoryItem}'s own
 * generic updatedBy/updatedAt, which says *something* changed but not what.
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_item_costing_method_change")
public class InventoryItemCostingMethodChange {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false, updatable = false)
    private InventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_method", nullable = false, updatable = false)
    private CostingMethod oldMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_method", nullable = false, updatable = false)
    private CostingMethod newMethod;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "changed_by", nullable = false, updatable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
