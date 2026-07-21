-- US-SEC-15 / FR-INT-05: the integration registry. Each row is one registered external
-- integration. credential_ref holds ONLY a secrets-manager reference (e.g. vault:path#key,
-- env:NAME, awssm:arn:...), NEVER a plaintext secret (US-SEC-15, AC-SEC-15-H) - the
-- application layer rejects any inline secret at 400 before it can be written here. config
-- carries non-secret settings only (also inline-secret-checked). Every integration is
-- disabled by default (FR-INT-05); enable/disable is recorded to the Security & Access Log.
CREATE TABLE integration (
    id            UUID PRIMARY KEY,
    name          VARCHAR(120) NOT NULL UNIQUE,
    type          VARCHAR(40)  NOT NULL,
    description   VARCHAR(500),
    credential_ref VARCHAR(500),                    -- a secrets-manager REFERENCE only, never a secret
    config        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_enabled    BOOLEAN      NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ  NOT NULL,
    created_by    UUID,
    updated_at    TIMESTAMPTZ
);
