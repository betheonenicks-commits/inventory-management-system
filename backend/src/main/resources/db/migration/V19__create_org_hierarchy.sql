-- US-ORG-01/02/06: configurable multi-level hierarchy with renameable level
-- labels (e.g. "Campus" -> "Parish") and Room-level variants (Classroom/
-- Laboratory). Levels are a small, ordered, renameable label set (rank
-- 0..N); nodes reference a level and an optional parent, forming the tree.
--
-- `path` is a materialized ancestor-id chain (including self), e.g.
-- '/<campusId>/<buildingId>/<roomId>/'. "Node X and everything under it" is
-- then a plain indexed prefix match (`path LIKE '<X's path>%'`) - no
-- recursive CTE needed. This is what FR-USR-04's descendant-scope
-- requirement, previously impossible without a hierarchy, needs. Nodes are
-- not re-parented in this phase, so path is computed once at creation and
-- never recomputed.
CREATE TABLE org_level (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(40)  NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    rank           INT          NOT NULL UNIQUE,
    room_variants  JSONB        NOT NULL DEFAULT '[]'::jsonb,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_by     UUID         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     UUID,
    updated_at     TIMESTAMPTZ
);

INSERT INTO org_level (code, name, rank, room_variants, created_by) VALUES
    ('CAMPUS',   'Campus',   0, '[]'::jsonb, '00000000-0000-0000-0000-0000000000ad'),
    ('BUILDING', 'Building', 1, '[]'::jsonb, '00000000-0000-0000-0000-0000000000ad'),
    ('FLOOR',    'Floor',    2, '[]'::jsonb, '00000000-0000-0000-0000-0000000000ad'),
    ('ROOM',     'Room',     3, '["Classroom", "Laboratory"]'::jsonb, '00000000-0000-0000-0000-0000000000ad');

ALTER TABLE org_node
    ADD COLUMN parent_id    UUID REFERENCES org_node(id),
    ADD COLUMN level_id     UUID REFERENCES org_level(id),
    ADD COLUMN path         TEXT,
    ADD COLUMN room_variant VARCHAR(50);

-- Backfill the pre-existing seeded root (V2) as the sole Campus-rank root of
-- the tree, so every FK already pointing at it (Asset/Person/AppUser
-- org-scope) keeps working unchanged.
UPDATE org_node
SET level_id = (SELECT id FROM org_level WHERE code = 'CAMPUS'),
    path = '/' || id || '/'
WHERE code = 'ROOT';

ALTER TABLE org_node
    ALTER COLUMN level_id SET NOT NULL,
    ALTER COLUMN path SET NOT NULL;

CREATE INDEX idx_org_node_path ON org_node (path text_pattern_ops);
CREATE INDEX idx_org_node_parent_id ON org_node (parent_id);
