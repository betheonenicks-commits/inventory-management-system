-- EPIC-LIF: procurement chain (US-LIF-01/02/03/16) - purchase request ->
-- approval -> PO -> line-level receiving/reconciliation, with partial
-- receipt, pre-receipt cancellation, and vendor returns as their own
-- immutable records.

CREATE TABLE purchase_request (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_description       VARCHAR(200) NOT NULL,
    justification          VARCHAR(1000) NOT NULL,
    estimated_cost         NUMERIC(12,2),
    vendor_name            VARCHAR(200),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    nominal_approver_id    UUID         NOT NULL REFERENCES app_user(id),
    effective_approver_id  UUID REFERENCES app_user(id),
    requested_by           UUID         NOT NULL,
    requested_at           TIMESTAMPTZ  NOT NULL,
    decided_by             UUID,
    decided_at             TIMESTAMPTZ,
    rejection_reason       VARCHAR(500),
    version                BIGINT       NOT NULL DEFAULT 0,
    created_by             UUID         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by             UUID,
    updated_at             TIMESTAMPTZ
);

CREATE INDEX idx_purchase_request_status ON purchase_request (status);

CREATE SEQUENCE purchase_order_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE purchase_order (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_number            VARCHAR(20)  NOT NULL UNIQUE,
    purchase_request_id  UUID         NOT NULL REFERENCES purchase_request(id),
    vendor_name          VARCHAR(200) NOT NULL,
    status               VARCHAR(15)  NOT NULL DEFAULT 'OPEN',
    version              BIGINT       NOT NULL DEFAULT 0,
    created_by           UUID         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by           UUID,
    updated_at           TIMESTAMPTZ
);

CREATE INDEX idx_purchase_order_request_id ON purchase_order (purchase_request_id);
CREATE INDEX idx_purchase_order_status ON purchase_order (status);

CREATE TABLE purchase_order_line (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id UUID         NOT NULL REFERENCES purchase_order(id),
    description       VARCHAR(200) NOT NULL,
    quantity_ordered  INTEGER      NOT NULL,
    quantity_received INTEGER      NOT NULL DEFAULT 0,
    quantity_returned INTEGER      NOT NULL DEFAULT 0,
    unit_cost         NUMERIC(12,2) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    version           BIGINT       NOT NULL DEFAULT 0,
    created_by        UUID         NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        UUID,
    updated_at        TIMESTAMPTZ
);

CREATE INDEX idx_purchase_order_line_order_id ON purchase_order_line (purchase_order_id);

-- US-LIF-16: the immutable record itself - one row per receipt/cancellation/return action.
CREATE TABLE purchase_order_line_event (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    line_id     UUID NOT NULL REFERENCES purchase_order_line(id),
    event_type  VARCHAR(20) NOT NULL,
    quantity    INTEGER,
    note        VARCHAR(500),
    actor_id    UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_purchase_order_line_event_line_id ON purchase_order_line_event (line_id);

-- US-LIF-01/02/03/16: same permission split as every other EPIC-LIF write -
-- Inventory Manager already holds assets:write/assets:read, Department Head
-- already holds approvals:write. No new permission strings needed.
