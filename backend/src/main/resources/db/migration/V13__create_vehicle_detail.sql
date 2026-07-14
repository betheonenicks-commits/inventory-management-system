-- Vehicle-specific attributes (FR-AST-15, US-AST-15), only relevant when an
-- asset's category is flagged as requiring them.
ALTER TABLE asset_category ADD COLUMN requires_vehicle_fields BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE vehicle_detail (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id                  UUID NOT NULL UNIQUE REFERENCES asset(id),
    vin                       VARCHAR(17) UNIQUE,
    registration_number       VARCHAR(20),
    odometer_reading          INTEGER CHECK (odometer_reading IS NULL OR odometer_reading >= 0),
    odometer_unit             VARCHAR(3)  NOT NULL DEFAULT 'MI',
    registration_expiry_date  DATE,
    insurance_expiry_date     DATE,
    version                   BIGINT      NOT NULL DEFAULT 0,
    created_by                UUID        NOT NULL,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by                UUID,
    updated_at                TIMESTAMPTZ
);

CREATE INDEX idx_vehicle_detail_registration_number ON vehicle_detail (registration_number);
