-- US-INV-06: closes the one remaining gap from EPIC-INV's first session -
-- "the change itself is recorded" (AC-INV-06-H) when an item's costing
-- method is switched, distinct from the generic updated_by/updated_at
-- InventoryItem already carries (that says *something* changed, not *what*
-- it changed from/to). Append-only, no version/updated_* columns - same
-- immutability discipline as audit_finding_correction/purchase_order_line_event.

CREATE TABLE inventory_item_costing_method_change (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_item_id   UUID NOT NULL REFERENCES inventory_item(id),
    old_method          VARCHAR(20) NOT NULL,
    new_method          VARCHAR(20) NOT NULL,
    changed_by          UUID NOT NULL,
    changed_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inventory_item_costing_method_change_item_id ON inventory_item_costing_method_change (inventory_item_id);
