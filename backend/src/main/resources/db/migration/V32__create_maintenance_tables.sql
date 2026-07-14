-- EPIC-LIF: repair logging (US-LIF-06) and preventive/corrective maintenance
-- (US-LIF-07/08). All three mutable (version + updated_*) - these are
-- current-state records that get closed/rescheduled in place, not an
-- append-only fact log like audit_finding.

CREATE TABLE repair_event (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id              UUID         NOT NULL REFERENCES asset(id),
    previous_status_code  VARCHAR(30)  NOT NULL,
    vendor_name           VARCHAR(200),
    reason                VARCHAR(500) NOT NULL,
    estimated_cost        NUMERIC(12,2),
    expected_return_date  DATE,
    actual_cost           NUMERIC(12,2),
    actual_return_date    DATE,
    status                VARCHAR(10)  NOT NULL DEFAULT 'OPEN',
    logged_by             UUID         NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    created_by            UUID         NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by            UUID,
    updated_at            TIMESTAMPTZ
);

CREATE INDEX idx_repair_event_asset_id ON repair_event (asset_id);
CREATE INDEX idx_repair_event_status ON repair_event (status);

CREATE TABLE maintenance_schedule (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id         UUID        NOT NULL REFERENCES asset(id),
    interval_months  INTEGER     NOT NULL,
    next_due_date    DATE        NOT NULL,
    description      VARCHAR(500),
    active           BOOLEAN     NOT NULL DEFAULT true,
    version          BIGINT      NOT NULL DEFAULT 0,
    created_by       UUID        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by       UUID,
    updated_at       TIMESTAMPTZ
);

CREATE INDEX idx_maintenance_schedule_asset_id ON maintenance_schedule (asset_id);
CREATE INDEX idx_maintenance_schedule_next_due_date ON maintenance_schedule (next_due_date) WHERE active = true;

CREATE TABLE maintenance_event (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id          UUID        NOT NULL REFERENCES asset(id),
    schedule_id       UUID REFERENCES maintenance_schedule(id),
    maintenance_type  VARCHAR(15) NOT NULL,
    performed_at      TIMESTAMPTZ NOT NULL,
    notes             VARCHAR(1000),
    cost              NUMERIC(12,2),
    performed_by      UUID        NOT NULL,
    version           BIGINT      NOT NULL DEFAULT 0,
    created_by        UUID        NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        UUID,
    updated_at        TIMESTAMPTZ
);

CREATE INDEX idx_maintenance_event_asset_id ON maintenance_event (asset_id);
CREATE INDEX idx_maintenance_event_schedule_id ON maintenance_event (schedule_id);

-- US-LIF-06/07/08: same permission split as transfers/disposals -
-- Inventory Manager already holds assets:write/assets:read (V15), no new
-- permission strings needed.
