-- Minimal placeholder for the future ORG module (FR-ORG-01). Asset.org_node_id
-- FKs here; the full hierarchy (multi-level, relabelable) is a later epic.
CREATE TABLE org_node (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Fixed literal UUID so backend defaults / tests / frontend fixtures can
-- reference a known root node deterministically.
INSERT INTO org_node (id, name, code, is_active)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Organization', 'ROOT', true);
