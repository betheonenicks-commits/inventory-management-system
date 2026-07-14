-- US-SEC-05: a configurable password policy. Single-row table (like
-- role_definition is multi-row per role, this is the one org-wide policy) -
-- PasswordPolicyRepository.findAll().findFirst() reads "the" policy;
-- PasswordPolicyService seeds this default row via migration rather than at
-- application startup, so it exists before any user is ever created.
-- Defaults match the previous hardcoded 8-char-minimum-only floor exactly -
-- this migration changes nothing about what's accepted until a Super
-- Administrator actually tightens it via the API.
CREATE TABLE password_policy (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    min_length         INT          NOT NULL DEFAULT 8,
    require_uppercase  BOOLEAN      NOT NULL DEFAULT false,
    require_lowercase  BOOLEAN      NOT NULL DEFAULT false,
    require_digit      BOOLEAN      NOT NULL DEFAULT false,
    require_special    BOOLEAN      NOT NULL DEFAULT false,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_by         UUID         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by         UUID,
    updated_at         TIMESTAMPTZ
);

INSERT INTO password_policy (min_length, created_by)
VALUES (8, '00000000-0000-0000-0000-0000000000ad');
