-- Flat, non-inheriting role assignment (FR-USR-07, US-USR-07): a user needing
-- multiple capabilities gets multiple rows here, and each role's own
-- permissions apply verbatim - nothing is implied or unioned beyond a plain
-- union of each assigned role's explicit permission set at read time.
CREATE TABLE user_role_assignment (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES app_user(id),
    role_id     UUID        NOT NULL REFERENCES role_definition(id),
    assigned_by UUID        NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_role_assignment_user_id ON user_role_assignment (user_id);
CREATE INDEX idx_user_role_assignment_role_id ON user_role_assignment (role_id);
