-- US-SEC-01: revocable refresh tokens. Only the SHA-256 hash of the raw
-- token is ever stored (never the raw value) - the same reasoning
-- password_hash already follows for passwords. Rotation-based: each
-- exchange revokes the presented row and issues a brand new one, so a
-- stolen-then-reused token is detectable (it will already be revoked when
-- the legitimate client's next rotation - or the attacker's replay -
-- presents it) rather than remaining silently valid until its natural
-- expiry. last_used_at (updated on every successful rotation) is what
-- US-SEC-06's idle-timeout check reads.
CREATE TABLE refresh_token (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES app_user(id),
    token_hash     VARCHAR(64)  NOT NULL UNIQUE,
    issued_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ  NOT NULL,
    last_used_at   TIMESTAMPTZ,
    revoked_at     TIMESTAMPTZ
);

CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);
