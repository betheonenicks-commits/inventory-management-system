-- US-RPT-13: recurring report delivery. Recipients are literal email
-- addresses (the AC's "3 recipients"), params is the same string-param map
-- the export-job path replays, stored as JSON. status leaves ACTIVE only
-- when the owner is deactivated (PAUSED_OWNER_DEACTIVATED) - the AC's
-- "pauses and flags for reassignment rather than failing silently forever".
CREATE TABLE report_schedule (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES app_user (id),
    report_key VARCHAR(40) NOT NULL,
    params TEXT NOT NULL DEFAULT '{}',
    export_format VARCHAR(6) NOT NULL,
    frequency VARCHAR(10) NOT NULL,
    recipients TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    next_run_at TIMESTAMPTZ NOT NULL,
    last_run_at TIMESTAMPTZ,
    last_outcome VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_report_schedule_due ON report_schedule (next_run_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_report_schedule_owner ON report_schedule (owner_user_id);
