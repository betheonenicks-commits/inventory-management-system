-- Core asset register (FR-AST-01 and the other Phase-1 AST stories). See
-- IAMS_Backend_Architecture_Specification_v1.1.md / IAMS_Data_Dictionary_v1.1_Amendment.md
-- for the full-system field set; this migration carries only the Phase-1 subset
-- (parent_asset_id and assigned_to_person_id columns are reserved now for
-- US-AST-04/LIF but not yet exercised by any endpoint).
CREATE SEQUENCE asset_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE asset (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_number                VARCHAR(50)  NOT NULL UNIQUE,
    name                        VARCHAR(160) NOT NULL,
    category_id                 UUID         NOT NULL REFERENCES asset_category(id),
    status_id                   UUID         NOT NULL REFERENCES asset_status_def(id),
    org_node_id                 UUID         NOT NULL REFERENCES org_node(id),
    assigned_to_person_id       UUID,
    parent_asset_id             UUID REFERENCES asset(id),
    serial_number               VARCHAR(150),
    manufacturer                VARCHAR(150),
    model                       VARCHAR(150),
    description                 TEXT,
    barcode_value                VARCHAR(100) NOT NULL UNIQUE,
    qr_payload                  VARCHAR(500) NOT NULL UNIQUE,
    custom_attributes           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    vendor_name                 VARCHAR(200),
    purchase_order_reference    VARCHAR(100),
    purchase_date                DATE,
    purchase_cost                NUMERIC(14,2) CHECK (purchase_cost IS NULL OR purchase_cost >= 0),
    warranty_start_date          DATE,
    warranty_end_date           DATE,
    version                     BIGINT       NOT NULL DEFAULT 0,
    created_by                  UUID         NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by                  UUID,
    updated_at                  TIMESTAMPTZ,
    CONSTRAINT chk_warranty_dates CHECK (warranty_start_date IS NULL OR warranty_end_date IS NULL OR warranty_end_date >= warranty_start_date)
);

CREATE INDEX idx_asset_serial_number ON asset (serial_number);
CREATE INDEX idx_asset_category_id ON asset (category_id);
CREATE INDEX idx_asset_status_id ON asset (status_id);
CREATE INDEX idx_asset_org_node_id ON asset (org_node_id);
CREATE INDEX idx_asset_custom_attributes ON asset USING GIN (custom_attributes);
CREATE INDEX idx_asset_warranty_end_date ON asset (warranty_end_date) WHERE warranty_end_date IS NOT NULL;
