package com.iams.inventory.domain;

import com.iams.common.domain.BaseEntity;
import com.iams.org.domain.OrgNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * US-INV-03: a physical stock location, org-scoped the same way {@code Asset}
 * is - {@link com.iams.usr.application.OrgScopeGuard} applies identically, so
 * a scoped Inventory Manager only sees/manages warehouses (and their stock)
 * within their own org-node subtree. Shelf/bin-level precision is not a
 * separate hierarchy here (unlike OrgNode's Campus/Building/Floor/Room levels,
 * which model organizational structure, not warehouse shelving) - it's the
 * free-text {@code subLocation} carried directly on
 * {@link InventoryStockBalance}/{@link InventoryTransaction}.
 */
@Getter
@Setter
@Entity
@Table(name = "warehouse")
public class Warehouse extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_node_id", nullable = false)
    private OrgNode orgNode;

    @Column(nullable = false)
    private boolean active = true;
}
