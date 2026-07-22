-- EPIC-MIG (Data Migration & Bulk Import/Export), first vertical slice: Asset bulk import.
-- US-MIG-01 (templates), US-MIG-03 (dry-run validate -> idempotent commit + reconciliation),
-- US-MIG-04 (import run history with role-scoped visibility).

-- New permission strings (checked by @perm.has). Two distinct capabilities, because
-- MIG's own ACs split them: an Inventory Manager RUNS imports (template/dry-run/commit)
-- but is explicitly REFUSED the import-history view (AC-MIG-04-X); that view is for
-- Super Admin / Admin / IT Security Officer (AC-MIG-04-H). SUPER_ADMIN needs neither
-- appended - its '["*"]' wildcard already matches both.
--   imports:write -> run an import (INVENTORY_MANAGER, ADMIN)
--   imports:read  -> browse import history (ADMIN, IT_SECURITY_OFFICER)
UPDATE role_definition SET permissions = permissions || '["imports:write"]'::jsonb
    WHERE code IN ('INVENTORY_MANAGER', 'ADMIN');
UPDATE role_definition SET permissions = permissions || '["imports:read"]'::jsonb
    WHERE code IN ('ADMIN', 'IT_SECURITY_OFFICER');

-- One row per import run. A dry-run creates it (status VALIDATED) with the per-row
-- error report and the validated payload; commit mutates the same row in place
-- (status COMMITTED) with the reconciliation counts, so the whole run - who/when/
-- counts/outcome - is one traceable record (US-MIG-04) and survives a browser close
-- (US-MIG-03 AC-X: the status/reconciliation stay retrievable server-side).
CREATE TABLE import_run (
    id                 UUID PRIMARY KEY,
    version            BIGINT       NOT NULL,
    entity_type        VARCHAR(40)  NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    template_version   VARCHAR(20),
    original_filename  VARCHAR(255),
    total_rows         INT          NOT NULL DEFAULT 0,
    valid_rows         INT          NOT NULL DEFAULT 0,
    invalid_rows       INT          NOT NULL DEFAULT 0,
    committed_rows     INT,
    failed_rows        INT,
    skipped_rows       INT,
    outcome            VARCHAR(60),
    -- The client-supplied key that makes commit idempotent (AC-MIG-03-H): a replayed
    -- commit with the same key returns the cached reconciliation instead of re-creating.
    idempotency_key    VARCHAR(100) UNIQUE,
    -- Per-row validation failures from the dry run: [{"rowNumber":n,"field":"...","message":"..."}]
    error_report       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    -- The rows that passed dry-run validation, held so commit needs no re-upload.
    valid_payload      JSONB        NOT NULL DEFAULT '[]'::jsonb,
    created_by         UUID         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_by         UUID,
    updated_at         TIMESTAMPTZ,
    committed_by       UUID,
    committed_at       TIMESTAMPTZ
);

CREATE INDEX idx_import_run_created_at ON import_run (created_at DESC);
