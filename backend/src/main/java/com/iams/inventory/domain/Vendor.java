package com.iams.inventory.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * US-INV-08: vendor CRUD independent of items. Deactivation (active=false) is
 * the only "removal" - never a delete, since {@code purchase_order.vendor_id}
 * (see EPIC-LIF's {@code PurchaseOrder}) must keep resolving to a real vendor
 * record for a deactivated vendor's historical purchase history (US-INV-07)
 * to remain intact.
 */
@Getter
@Setter
@Entity
@Table(name = "vendor")
public class Vendor extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(nullable = false)
    private boolean active = true;
}
