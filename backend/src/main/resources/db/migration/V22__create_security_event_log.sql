-- US-SEC-04: a complete, append-only Security & Access Log. actor_user_id is
-- nullable - a failed login against an unknown username has no actor to
-- attribute it to, but the attempt itself (with the attempted username,
-- stored here for investigative value even though the API response never
-- reveals whether that username exists - AC-SEC-04-X) must still be
-- recorded. No update/delete path exists anywhere in the application code;
-- immutability is structural (no version, no updated_* columns), the same
-- pattern asset_history_event already established.
CREATE TABLE security_event_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type          VARCHAR(40)  NOT NULL,
    actor_user_id       UUID REFERENCES app_user(id),
    username_attempted  VARCHAR(60),
    ip_address          VARCHAR(45),
    detail              VARCHAR(500),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_security_event_log_actor_user_id ON security_event_log (actor_user_id);
CREATE INDEX idx_security_event_log_event_type ON security_event_log (event_type);
CREATE INDEX idx_security_event_log_created_at ON security_event_log (created_at);
