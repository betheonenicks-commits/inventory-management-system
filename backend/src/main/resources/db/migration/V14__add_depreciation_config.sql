-- Depreciation & net book value (FR-AST-16, US-AST-16). Category defaults +
-- an optional per-asset override, where any null override field falls back
-- to the category default for that field. Computed on demand - no persisted
-- per-period entry table, since nothing consumes historical entries yet
-- (no report epic exists to read them).
ALTER TABLE asset_category
    ADD COLUMN default_depreciation_method VARCHAR(30),
    ADD COLUMN default_useful_life_months INTEGER CHECK (default_useful_life_months IS NULL OR default_useful_life_months > 0),
    ADD COLUMN default_salvage_value_pct NUMERIC(5,2) CHECK (default_salvage_value_pct IS NULL OR (default_salvage_value_pct >= 0 AND default_salvage_value_pct <= 100));

CREATE TABLE asset_depreciation_override (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id                  UUID NOT NULL UNIQUE REFERENCES asset(id),
    method                    VARCHAR(30),
    useful_life_months        INTEGER CHECK (useful_life_months IS NULL OR useful_life_months > 0),
    salvage_value_pct         NUMERIC(5,2) CHECK (salvage_value_pct IS NULL OR (salvage_value_pct >= 0 AND salvage_value_pct <= 100)),
    depreciation_start_date   DATE,
    version                   BIGINT      NOT NULL DEFAULT 0,
    created_by                UUID        NOT NULL,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by                UUID,
    updated_at                TIMESTAMPTZ
);
