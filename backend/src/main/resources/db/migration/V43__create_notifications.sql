-- EPIC-NTF: in-app notifications (the always-available channel, US-NTF-03),
-- per-user channel preferences (US-NTF-05), per-channel delivery tracking
-- with retry state (US-NTF-08), versioned templates (US-NTF-09), and the
-- exactly-once-per-threshold trigger ledger (US-NTF-06).

CREATE TABLE notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    event_type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    -- In-app route for US-NTF-10's deep link (e.g. /transfers). Nullable: not
    -- every notification has a landing page.
    resource_path VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ
);
CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC);
CREATE INDEX idx_notification_user_unread ON notification (user_id) WHERE read_at IS NULL;

CREATE TABLE notification_preference (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    event_type VARCHAR(40) NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_notification_pref UNIQUE (user_id, event_type)
);

-- Versioned, never edited in place: a new version row per save (US-NTF-09),
-- so past-sent notifications keep the wording they were actually sent with.
CREATE TABLE notification_template (
    id UUID PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    channel VARCHAR(10) NOT NULL,
    version INT NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    CONSTRAINT uq_notification_template UNIQUE (event_type, channel, version)
);

CREATE TABLE notification_delivery (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notification (id),
    channel VARCHAR(10) NOT NULL,
    -- Rendered at dispatch time from that channel's then-latest template
    -- version, so retries and history always use the wording of record.
    rendered_subject VARCHAR(200) NOT NULL,
    rendered_body TEXT NOT NULL,
    status VARCHAR(12) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    next_attempt_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_delivery_due ON notification_delivery (next_attempt_at)
    WHERE status IN ('PENDING', 'FAILED');

-- US-NTF-06's "exactly once per threshold": one row per (event, entity,
-- threshold); the unique constraint IS the dedup, so concurrent sweeps
-- cannot double-fire.
CREATE TABLE notification_trigger_log (
    id UUID PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    threshold_key VARCHAR(60) NOT NULL,
    fired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_trigger_once UNIQUE (event_type, entity_id, threshold_key)
);

-- Template administration permission (US-NTF-09), granted to admins only.
UPDATE role_definition
SET permissions = permissions || '["notifications:manage"]'::jsonb
WHERE code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT permissions @> '["notifications:manage"]'::jsonb;

-- Default v1 templates for every catalog event, both channels. {{variable}}
-- placeholders are substituted at send time; a missing variable renders as
-- an explicit [missing: name] marker rather than a broken message.
INSERT INTO notification_template (id, event_type, channel, version, subject, body) VALUES
 (gen_random_uuid(), 'UPCOMING_AUDIT', 'IN_APP', 1, 'Audit "{{auditName}}" starts {{when}}', 'Audit "{{auditName}}" is scheduled for {{scheduledDate}}.'),
 (gen_random_uuid(), 'UPCOMING_AUDIT', 'EMAIL', 1, '[IAMS] Audit "{{auditName}}" starts {{when}}', 'Audit "{{auditName}}" is scheduled for {{scheduledDate}}. Sign in to IAMS to review its scope and assignments.'),
 (gen_random_uuid(), 'OVERDUE_AUDIT', 'IN_APP', 1, 'Audit "{{auditName}}" is overdue', 'Audit "{{auditName}}" was scheduled for {{scheduledDate}} and is still open ({{daysOverdue}} day(s) overdue).'),
 (gen_random_uuid(), 'OVERDUE_AUDIT', 'EMAIL', 1, '[IAMS] Audit "{{auditName}}" is overdue', 'Audit "{{auditName}}" was scheduled for {{scheduledDate}} and is still open ({{daysOverdue}} day(s) overdue). This reminder repeats until the audit closes.'),
 (gen_random_uuid(), 'EXPIRY', 'IN_APP', 1, '{{kind}} for {{assetName}} due {{dueDate}}', '{{kind}} for asset {{assetName}} is due on {{dueDate}}. {{detail}}'),
 (gen_random_uuid(), 'EXPIRY', 'EMAIL', 1, '[IAMS] {{kind}} for {{assetName}} due {{dueDate}}', '{{kind}} for asset {{assetName}} is due on {{dueDate}}. {{detail}}'),
 (gen_random_uuid(), 'MAINTENANCE_DUE', 'IN_APP', 1, 'Maintenance due for {{assetName}}', 'Maintenance for asset {{assetName}} is due on {{dueDate}}. {{detail}}'),
 (gen_random_uuid(), 'MAINTENANCE_DUE', 'EMAIL', 1, '[IAMS] Maintenance due for {{assetName}}', 'Maintenance for asset {{assetName}} is due on {{dueDate}}. {{detail}}'),
 (gen_random_uuid(), 'LOW_STOCK', 'IN_APP', 1, 'Low stock: {{itemName}}', '{{itemName}} is at {{quantity}} {{unit}}, below its reorder level of {{reorderLevel}}.'),
 (gen_random_uuid(), 'LOW_STOCK', 'EMAIL', 1, '[IAMS] Low stock: {{itemName}}', '{{itemName}} is at {{quantity}} {{unit}}, below its reorder level of {{reorderLevel}}. Consider raising a purchase request.'),
 (gen_random_uuid(), 'PENDING_APPROVAL', 'IN_APP', 1, '{{entityType}} awaiting your approval', '{{summary}} has been waiting for your approval since {{since}}.'),
 (gen_random_uuid(), 'PENDING_APPROVAL', 'EMAIL', 1, '[IAMS] {{entityType}} awaiting your approval', '{{summary}} has been waiting for your approval since {{since}}. Sign in to IAMS to approve or reject it.'),
 (gen_random_uuid(), 'SECURITY_ALERT', 'IN_APP', 1, 'Security alert: {{summary}}', '{{detail}}'),
 (gen_random_uuid(), 'SECURITY_ALERT', 'EMAIL', 1, '[IAMS] Security alert: {{summary}}', '{{detail}}'),
 (gen_random_uuid(), 'ASSIGNMENT', 'IN_APP', 1, 'Asset {{assetNumber}} {{action}}', 'Asset {{assetNumber}} ({{assetName}}) was {{action}} {{personName}} by {{actor}}.'),
 (gen_random_uuid(), 'ASSIGNMENT', 'EMAIL', 1, '[IAMS] Asset {{assetNumber}} {{action}}', 'Asset {{assetNumber}} ({{assetName}}) was {{action}} {{personName}} by {{actor}}.'),
 (gen_random_uuid(), 'TRANSFER_DECISION', 'IN_APP', 1, 'Transfer {{decision}}: {{assetName}}', 'The transfer request for {{assetName}} was {{decision}} by {{actor}}. {{reason}}'),
 (gen_random_uuid(), 'TRANSFER_DECISION', 'EMAIL', 1, '[IAMS] Transfer {{decision}}: {{assetName}}', 'The transfer request for {{assetName}} was {{decision}} by {{actor}}. {{reason}}');
