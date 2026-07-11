-- Configurable status list (FR-AST-07). Admin-editable in a later pass; seeded
-- here with the seven statuses the FRS names explicitly.
CREATE TABLE asset_status_def (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(30)  NOT NULL UNIQUE,
    label       VARCHAR(100) NOT NULL,
    is_terminal BOOLEAN      NOT NULL DEFAULT false,
    sort_order  INTEGER      NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by  UUID,
    updated_at  TIMESTAMPTZ
);

INSERT INTO asset_status_def (id, code, label, is_terminal, sort_order, created_by)
VALUES
    (gen_random_uuid(), 'IN_USE',      'In Use',      false, 1, '00000000-0000-0000-0000-0000000000ad'),
    (gen_random_uuid(), 'IN_STORAGE',  'In Storage',  false, 2, '00000000-0000-0000-0000-0000000000ad'),
    (gen_random_uuid(), 'UNDER_REPAIR','Under Repair',false, 3, '00000000-0000-0000-0000-0000000000ad'),
    (gen_random_uuid(), 'MISSING',     'Missing',     false, 4, '00000000-0000-0000-0000-0000000000ad'),
    (gen_random_uuid(), 'RETIRED',     'Retired',     true,  5, '00000000-0000-0000-0000-0000000000ad'),
    (gen_random_uuid(), 'DISPOSED',    'Disposed',    true,  6, '00000000-0000-0000-0000-0000000000ad'),
    (gen_random_uuid(), 'VOID',        'Void',        true,  7, '00000000-0000-0000-0000-0000000000ad');
