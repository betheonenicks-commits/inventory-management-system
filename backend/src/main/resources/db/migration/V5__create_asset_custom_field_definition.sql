-- Per-category custom field schema (FR-AST-06), modeled as a reference table
-- of typed field definitions rather than a JSON-Schema-in-column blob -
-- matches the reconciled Backend Architecture Spec v1.1 and the frontend's
-- "dynamic field builder" (label/type/required per row).
CREATE TABLE asset_custom_field_definition (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id     UUID NOT NULL REFERENCES asset_category(id),
    field_key       VARCHAR(100) NOT NULL,
    label           VARCHAR(150) NOT NULL,
    data_type       VARCHAR(20)  NOT NULL CHECK (data_type IN ('TEXT','NUMBER','DATE','BOOLEAN','ENUM')),
    is_required     BOOLEAN      NOT NULL DEFAULT false,
    enum_options    JSONB,
    display_order   INTEGER      NOT NULL DEFAULT 0,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ,
    CONSTRAINT uq_custom_field_category_key UNIQUE (category_id, field_key)
);
