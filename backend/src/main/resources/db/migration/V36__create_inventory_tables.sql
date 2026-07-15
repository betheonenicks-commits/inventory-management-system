-- EPIC-INV: quantity-based inventory (BR-06), distinct from individually
-- tracked assets (US-INV-01). Six tables: warehouses (org-scoped, same
-- pattern as asset), vendors (independent of items - US-INV-08), the item
-- catalog, the current-balance table (one row per item/warehouse/sub-location/
-- lot combination - US-INV-03/09), the append-only movement ledger every
-- balance change is derived from (US-INV-02, mirrors asset_history_event's
-- "mutable current state + immutable trail" split), and the manual-adjustment
-- approval workflow (US-INV-05, mirrors asset_disposal_request's shape).

CREATE TABLE warehouse (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(200) NOT NULL,
    code          VARCHAR(40)  NOT NULL UNIQUE,
    org_node_id   UUID         NOT NULL REFERENCES org_node(id),
    active        BOOLEAN      NOT NULL DEFAULT true,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_by    UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by    UUID,
    updated_at    TIMESTAMPTZ
);

CREATE INDEX idx_warehouse_org_node_id ON warehouse (org_node_id);

-- US-INV-08: vendor CRUD independent of items - deactivation is a flag, never
-- a delete, so historical purchase-order links (see purchase_order.vendor_id
-- below) always keep resolving.
CREATE TABLE vendor (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(200) NOT NULL,
    contact_email  VARCHAR(200),
    contact_phone  VARCHAR(40),
    active         BOOLEAN      NOT NULL DEFAULT true,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_by     UUID         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     UUID,
    updated_at     TIMESTAMPTZ
);

CREATE TABLE inventory_item (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    sku             VARCHAR(60)  NOT NULL UNIQUE,
    unit_of_measure VARCHAR(20)  NOT NULL,
    reorder_level   NUMERIC(14,3),
    costing_method  VARCHAR(20)  NOT NULL DEFAULT 'WEIGHTED_AVERAGE',
    active          BOOLEAN      NOT NULL DEFAULT true,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ
);

-- US-INV-01/03/09: the current balance, one row per distinct item/warehouse/
-- sub-location/lot combination. sub_location and lot_number default to '' (not
-- NULL) specifically so the natural key below can be a plain UNIQUE constraint -
-- a non-lot-tracked item just has lot_number = ''. expiry_date is only ever set
-- alongside a real lot_number (US-INV-09); average_unit_cost is maintained here
-- rather than recomputed per query (US-INV-06's weighted-average, recalculated
-- on each receipt per its own AC).
CREATE TABLE inventory_stock_balance (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_item_id  UUID NOT NULL REFERENCES inventory_item(id),
    warehouse_id       UUID NOT NULL REFERENCES warehouse(id),
    sub_location       VARCHAR(100) NOT NULL DEFAULT '',
    lot_number         VARCHAR(100) NOT NULL DEFAULT '',
    expiry_date        DATE,
    quantity_on_hand   NUMERIC(14,3) NOT NULL DEFAULT 0,
    average_unit_cost  NUMERIC(14,4),
    version            BIGINT NOT NULL DEFAULT 0,
    UNIQUE (inventory_item_id, warehouse_id, sub_location, lot_number),
    CONSTRAINT chk_stock_balance_non_negative CHECK (quantity_on_hand >= 0)
);

CREATE INDEX idx_inventory_stock_balance_item_id ON inventory_stock_balance (inventory_item_id);
CREATE INDEX idx_inventory_stock_balance_warehouse_id ON inventory_stock_balance (warehouse_id);
CREATE INDEX idx_inventory_stock_balance_expiry_date ON inventory_stock_balance (expiry_date) WHERE expiry_date IS NOT NULL;

-- US-INV-02: every movement, append-only - no version, no updated_* columns,
-- the same immutability discipline as asset_history_event/audit_finding.
-- linked_transaction_id pairs a TRANSFER_OUT with its TRANSFER_IN (US-INV-08's
-- "atomic linked pair"), self-referencing this same table. currency_code/
-- fx_rate/reporting_unit_cost (US-INV-10) are only ever populated on STOCK_IN -
-- captured once at entry and never recalculated against a later rate.
CREATE TABLE inventory_transaction (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_item_id     UUID NOT NULL REFERENCES inventory_item(id),
    warehouse_id          UUID NOT NULL REFERENCES warehouse(id),
    sub_location          VARCHAR(100) NOT NULL DEFAULT '',
    lot_number            VARCHAR(100) NOT NULL DEFAULT '',
    expiry_date           DATE,
    transaction_type      VARCHAR(20)  NOT NULL,
    quantity              NUMERIC(14,3) NOT NULL,
    unit_cost             NUMERIC(14,4),
    currency_code         VARCHAR(3),
    fx_rate               NUMERIC(14,6),
    reporting_unit_cost   NUMERIC(14,4),
    reason_code           VARCHAR(200) NOT NULL,
    performed_by_user_id  UUID NOT NULL,
    performed_by_username VARCHAR(60) NOT NULL,
    performed_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    linked_transaction_id UUID REFERENCES inventory_transaction(id),
    CONSTRAINT chk_inventory_transaction_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_inventory_transaction_item_id ON inventory_transaction (inventory_item_id);
CREATE INDEX idx_inventory_transaction_warehouse_id ON inventory_transaction (warehouse_id);
CREATE INDEX idx_inventory_transaction_performed_at ON inventory_transaction (performed_at);

-- US-INV-05: a manual adjustment changes quantity only after approval -
-- mirrors asset_disposal_request's request/approve/reject shape exactly,
-- reusing the same effective-approver-delegation resolution (ApprovalRoutingService).
CREATE TABLE inventory_manual_adjustment_request (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_item_id        UUID NOT NULL REFERENCES inventory_item(id),
    warehouse_id             UUID NOT NULL REFERENCES warehouse(id),
    sub_location             VARCHAR(100) NOT NULL DEFAULT '',
    lot_number               VARCHAR(100) NOT NULL DEFAULT '',
    quantity_delta           NUMERIC(14,3) NOT NULL,
    reason                   VARCHAR(1000) NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    nominal_approver_id      UUID NOT NULL REFERENCES app_user(id),
    effective_approver_id    UUID REFERENCES app_user(id),
    requested_by             UUID NOT NULL,
    requested_at             TIMESTAMPTZ NOT NULL,
    decided_by                UUID,
    decided_at                TIMESTAMPTZ,
    rejection_reason          VARCHAR(500),
    resulting_transaction_id  UUID REFERENCES inventory_transaction(id),
    version                   BIGINT NOT NULL DEFAULT 0,
    created_by                UUID NOT NULL,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by                UUID,
    updated_at                TIMESTAMPTZ,
    CONSTRAINT chk_manual_adjustment_quantity_delta_nonzero CHECK (quantity_delta <> 0)
);

CREATE INDEX idx_inventory_manual_adjustment_status ON inventory_manual_adjustment_request (status);

-- US-INV-07: a real FK, not the free-text vendor_name purchase_order already
-- had from EPIC-LIF's own session (before any Vendor entity existed) - kept
-- alongside it rather than replacing it, since vendor_name remains a valid
-- free-text purchase even when the vendor isn't (yet) a registered Vendor record.
ALTER TABLE purchase_order ADD COLUMN vendor_id UUID REFERENCES vendor(id);
CREATE INDEX idx_purchase_order_vendor_id ON purchase_order (vendor_id) WHERE vendor_id IS NOT NULL;

-- INVENTORY_MANAGER already holds inventory:read/inventory:write from V15 -
-- no permission grant needed. Manual-adjustment approval reuses
-- Department Head's existing approvals:write, same as every other
-- request/approve workflow in this codebase.
