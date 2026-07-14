-- Insurance policy tracking (FR-AST-14, US-AST-14). Trimmed from the DD's
-- full model: single current-policy row per asset, no separate claims/
-- renewal-history table (nothing in AC-AST-14 exercises that).
CREATE TABLE asset_insurance_detail (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id             UUID NOT NULL UNIQUE REFERENCES asset(id),
    insurer_name         VARCHAR(200),
    policy_number        VARCHAR(100),
    coverage_amount      NUMERIC(14,2) CHECK (coverage_amount IS NULL OR coverage_amount >= 0),
    coverage_currency    CHAR(3),
    policy_start_date    DATE,
    policy_expiry_date   DATE,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_by           UUID         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by           UUID,
    updated_at           TIMESTAMPTZ,
    CONSTRAINT chk_insurance_dates CHECK (policy_start_date IS NULL OR policy_expiry_date IS NULL OR policy_expiry_date >= policy_start_date)
);

CREATE INDEX idx_asset_insurance_detail_expiry ON asset_insurance_detail (policy_expiry_date) WHERE policy_expiry_date IS NOT NULL;
