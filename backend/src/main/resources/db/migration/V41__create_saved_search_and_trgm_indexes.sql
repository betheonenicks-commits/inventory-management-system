-- EPIC-SRC, two things:
--
-- 1. saved_search (US-SRC-04): one row per user-named filter combination over
--    the asset search (US-SRC-03's parameters). References are stored as
--    plain UUIDs with NO foreign keys, deliberately: the story's own AC
--    requires a saved search referencing a since-deleted category to degrade
--    gracefully at re-apply time (clause dropped, noted) - an FK would either
--    block that deletion or cascade-destroy the saved search, both of which
--    contradict the AC. Existence is checked at resolve time instead.
--
-- 2. pg_trgm GIN indexes (closes US-SRC-01's Partial): the global search's
--    free-text legs are substring LIKEs, which scan without trigram indexes.
--    pg_trgm is a TRUSTED extension (PG 13+), so CREATE EXTENSION needs only
--    database-owner privilege, which iams_app holds - verified live against
--    this deployment's native PostgreSQL 18 before this migration was
--    written, not assumed.

CREATE TABLE saved_search (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES app_user(id),
    name            VARCHAR(120) NOT NULL,
    query           VARCHAR(255),
    category_id     UUID,
    status_id       UUID,
    org_node_id     UUID,
    purchased_from  DATE,
    purchased_to    DATE,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ,
    CONSTRAINT uq_saved_search_user_name UNIQUE (user_id, name)
);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_asset_name_trgm ON asset USING gin (lower(name) gin_trgm_ops);
CREATE INDEX idx_asset_number_trgm ON asset USING gin (lower(asset_number) gin_trgm_ops);
CREATE INDEX idx_asset_serial_trgm ON asset USING gin (lower(coalesce(serial_number, '')) gin_trgm_ops);
CREATE INDEX idx_asset_rfid_trgm ON asset USING gin (lower(coalesce(rfid_tag_id, '')) gin_trgm_ops);
CREATE INDEX idx_vendor_name_trgm ON vendor USING gin (lower(name) gin_trgm_ops);
CREATE INDEX idx_person_full_name_trgm ON person USING gin (lower(full_name) gin_trgm_ops);
