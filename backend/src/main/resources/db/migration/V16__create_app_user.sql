-- Real user accounts (FR-USR-01), replacing the DevSecurityProperties.DevUser
-- hardcoded single-user stub that AuthController used until now. person_id is
-- a plain UUID reference (not a JPA relation), matching the same convention
-- asset.assigned_to_person_id already uses - a login account may optionally
-- correspond to a Person (FR-ORG-04: a Person doesn't require a login, and
-- symmetrically a User isn't required to have a Person record, e.g. a
-- system-only account). org_scope_node_id is nullable: an unscoped role
-- (typically SUPER_ADMIN) has no restriction to enforce.
--
-- No row is seeded here for the bootstrap Super Administrator - see
-- BootstrapUserSeeder, which creates it at application startup using the
-- injected PasswordEncoder bean so the password hash is computed by the same
-- encoder used to verify it at login, rather than a hand-computed hash
-- pasted into SQL. created_by stays NOT NULL like every other BaseEntity
-- table (no nullable-audit-column special case): the seeder sets the
-- bootstrap row's created_by to its own generated id, since "the first
-- Super Administrator created itself" is a cleaner invariant to keep than
-- widening the column for one row.
CREATE TABLE app_user (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username          VARCHAR(60)  NOT NULL UNIQUE,
    password_hash     VARCHAR(100) NOT NULL,
    display_name      VARCHAR(120) NOT NULL,
    email             VARCHAR(255),
    person_id         UUID REFERENCES person(id),
    org_scope_node_id UUID REFERENCES org_node(id),
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version           BIGINT       NOT NULL DEFAULT 0,
    created_by        UUID         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by        UUID,
    updated_at        TIMESTAMPTZ
);

CREATE INDEX idx_app_user_org_scope_node_id ON app_user (org_scope_node_id);
CREATE INDEX idx_app_user_person_id ON app_user (person_id);
