-- US-USR-09: a recorded Separation-of-Duties waiver for small organizations
-- that can't fully separate a control (e.g. a single-admin site). `scope` is
-- a free-form code (e.g. "AUDIT_APPROVAL") rather than an enum, since the
-- set of waivable conflicts grows with epics that don't exist yet (US-AUD-22's
-- reroute path). Recording and activation are built here; actually engaging
-- the reroute path on a submission conflict is deferred until that epic
-- exists - see DEVELOPMENT_LOG.md 2026-07-13.
CREATE TABLE sod_waiver (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope          VARCHAR(60)  NOT NULL,
    signed_off_by  UUID         NOT NULL REFERENCES app_user(id),
    reason         VARCHAR(500) NOT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT true,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_by     UUID         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     UUID,
    updated_at     TIMESTAMPTZ
);
