-- US-SEC-09: self-service account unlock. A locked-out user can't authenticate
-- to prove who they are, so a one-time emailed token stands in for a session -
-- same hashed-token idiom as refresh_token (only the SHA-256 hash is stored,
-- never the raw value the user actually received).
CREATE TABLE account_unlock_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_account_unlock_token_user ON account_unlock_token (user_id);
