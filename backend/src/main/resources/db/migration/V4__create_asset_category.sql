-- Configurable asset categories (FR-AST-03).
CREATE TABLE asset_category (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_category_id  UUID REFERENCES asset_category(id),
    name                VARCHAR(150) NOT NULL UNIQUE,
    code                VARCHAR(50)  NOT NULL UNIQUE,
    is_active           BOOLEAN      NOT NULL DEFAULT true,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_by          UUID         NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by          UUID,
    updated_at          TIMESTAMPTZ
);

INSERT INTO asset_category (id, name, code, is_active, created_by)
VALUES ('00000000-0000-0000-0000-000000000010', 'General Equipment', 'GENERAL', true, '00000000-0000-0000-0000-0000000000ad');
