-- US-RPT-15: user-built, saved ad hoc reports. Same design decisions as
-- saved_search (V41): reference columns carry NO foreign keys because the
-- AC requires graceful degradation when a referenced entity (or a custom
-- field definition) is later removed - existence is checked at run time,
-- never enforced against future deletions. fields is the ordered list of
-- chosen field-catalog keys (built-ins plus "custom:<fieldKey>").
CREATE TABLE adhoc_report (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES app_user(id),
    name            VARCHAR(120) NOT NULL,
    fields          JSONB        NOT NULL,
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
    CONSTRAINT uq_adhoc_report_user_name UNIQUE (user_id, name)
);
