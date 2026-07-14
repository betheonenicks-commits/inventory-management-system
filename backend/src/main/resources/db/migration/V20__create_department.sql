-- US-ORG-03: departments/cost centers as their own dimension, independent of
-- the physical org_node hierarchy (a department can span multiple
-- buildings, or have no physical node at all). Linking Asset/Person to a
-- department is deliberately NOT done in this migration - see
-- DEVELOPMENT_LOG.md 2026-07-13 for why that's a separate, deferred step
-- rather than bundled in here.
CREATE TABLE department (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(200) NOT NULL,
    cost_center_code VARCHAR(50)  NOT NULL UNIQUE,
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_by       UUID         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by       UUID,
    updated_at       TIMESTAMPTZ
);
