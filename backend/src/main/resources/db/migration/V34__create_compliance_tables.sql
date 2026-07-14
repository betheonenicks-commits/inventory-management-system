-- EPIC-CMP: retention policies + purge (US-CMP-01), legal holds (US-CMP-06),
-- privacy notices (US-CMP-03), accessibility audit status (US-CMP-04), and
-- the data-residency / outbound-integration registry (US-CMP-05).

CREATE TABLE retention_policy (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type            VARCHAR(30)  NOT NULL UNIQUE,
    retention_period_days  INTEGER      NOT NULL,
    expiry_action          VARCHAR(20)  NOT NULL,
    version                BIGINT       NOT NULL DEFAULT 0,
    created_by             UUID         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by             UUID,
    updated_at             TIMESTAMPTZ
);

CREATE TABLE legal_hold (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type   VARCHAR(10)  NOT NULL,
    scope_id     UUID         NOT NULL,
    reason       VARCHAR(500) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT true,
    lifted_by    UUID,
    lifted_at    TIMESTAMPTZ,
    lift_reason  VARCHAR(500),
    version      BIGINT       NOT NULL DEFAULT 0,
    created_by   UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by   UUID,
    updated_at   TIMESTAMPTZ
);

CREATE INDEX idx_legal_hold_scope ON legal_hold (scope_type, scope_id);

CREATE TABLE privacy_notice_config (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_name  VARCHAR(100) NOT NULL UNIQUE,
    notice_text VARCHAR(1000) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by  UUID,
    updated_at  TIMESTAMPTZ
);

CREATE TABLE accessibility_audit_record (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_date  DATE         NOT NULL,
    outcome     VARCHAR(20)  NOT NULL,
    notes       VARCHAR(1000),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by  UUID,
    updated_at  TIMESTAMPTZ
);

CREATE TABLE outbound_integration_flag (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(100) NOT NULL UNIQUE,
    enabled                 BOOLEAN      NOT NULL DEFAULT false,
    compliance_review_note VARCHAR(1000),
    version                 BIGINT      NOT NULL DEFAULT 0,
    created_by              UUID        NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by              UUID,
    updated_at              TIMESTAMPTZ
);

-- US-CMP-02: the stable-pseudonym marker - the person's id/history/audit
-- references are untouched, only PII fields are overwritten at anonymization time.
ALTER TABLE person ADD COLUMN anonymized_at TIMESTAMPTZ;

-- US-CMP-01/02/03/04/05/06: all gated compliance:read/compliance:write, both
-- already granted to COMPLIANCE_OFFICER (V15) and covered by SUPER_ADMIN's
-- wildcard - no new permission strings needed.
