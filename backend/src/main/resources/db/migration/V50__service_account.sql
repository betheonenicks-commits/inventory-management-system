-- US-SEC-14 (AC-SEC-14-H) + US-SEC-15: scoped service-account credentials for
-- integrations. The credential is an API key; only its SHA-256 hash is stored
-- (US-SEC-15 - never plaintext), exactly like refresh tokens. Scopes (e.g.
-- INT_ACCOUNTING_READ) are the ONLY capability a service account has - it holds
-- no normal permissions, so it is refused every human endpoint by default and
-- may reach only the integration endpoints explicitly opened to a scope it holds.
CREATE TABLE service_account (
    id             UUID PRIMARY KEY,
    name           VARCHAR(100) NOT NULL UNIQUE,
    description    VARCHAR(500),
    api_key_hash   VARCHAR(64)  NOT NULL UNIQUE,   -- SHA-256 hex of the raw key
    api_key_prefix VARCHAR(16)  NOT NULL,          -- non-secret, for identification in listings
    scopes         JSONB        NOT NULL DEFAULT '[]'::jsonb,
    is_active      BOOLEAN      NOT NULL DEFAULT true,
    last_used_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    created_by     UUID
);

CREATE INDEX idx_service_account_api_key_hash ON service_account (api_key_hash);
