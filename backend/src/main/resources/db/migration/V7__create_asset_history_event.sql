-- Append-only change history (FR-AST-10). No version column, no updated_*
-- columns, and deliberately no controller endpoint is ever registered that
-- could UPDATE or DELETE a row here - immutability is structural, not a
-- privilege grant to revoke later.
CREATE TABLE asset_history_event (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id                UUID NOT NULL REFERENCES asset(id),
    event_type              VARCHAR(30) NOT NULL CHECK (event_type IN
        ('STATUS_CHANGE','LOCATION_CHANGE','ASSIGNMENT_CHANGE','CONDITION_CHANGE','FIELD_UPDATE','LIFECYCLE_EVENT','CORRECTION')),
    field_name              VARCHAR(100),
    old_value               TEXT,
    new_value               TEXT,
    correction_of_event_id  UUID REFERENCES asset_history_event(id),
    created_by              UUID        NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_asset_history_asset_id_created_at ON asset_history_event (asset_id, created_at DESC);
