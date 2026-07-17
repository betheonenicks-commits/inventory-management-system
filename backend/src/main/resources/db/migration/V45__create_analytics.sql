-- EPIC-ANL: deployment-local product analytics (BR-20/BO-5). Both tables are
-- append-only records; nothing in the analytics module has an outbound
-- network path, by design and by test (US-ANL-02 / AnalyticsSovereigntyTest).

-- US-ANL-01: server-side feature-usage events. One row per role the actor
-- held at action time - "module, action, and role" stays flat and exactly
-- aggregatable even for multi-role users (a user with two roles counts
-- toward both roles' adoption).
CREATE TABLE usage_event (
    id UUID PRIMARY KEY,
    module VARCHAR(40) NOT NULL,
    action VARCHAR(60) NOT NULL,
    role VARCHAR(40) NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user (id),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_usage_event_occurred ON usage_event (occurred_at);
CREATE INDEX idx_usage_event_role_module ON usage_event (role, module);

-- US-ANL-04: submitted feedback. The routed notification carries the content
-- to the configured recipient; this row is the durable record of receipt.
CREATE TABLE feedback_item (
    id UUID PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    message TEXT,
    page_path VARCHAR(255),
    submitted_by UUID NOT NULL REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- US-ANL-04 routing rides on EPIC-NTF's catalog: v1 templates for the new
-- FEEDBACK_RECEIVED event, both channels (see V43 for the placeholder rules).
INSERT INTO notification_template (id, event_type, channel, version, subject, body) VALUES
 (gen_random_uuid(), 'FEEDBACK_RECEIVED', 'IN_APP', 1, 'Feedback received: {{category}}',
  '{{submitter}} submitted {{category}} feedback{{pageContext}}. {{message}}'),
 (gen_random_uuid(), 'FEEDBACK_RECEIVED', 'EMAIL', 1, '[IAMS] Feedback received: {{category}}',
  '{{submitter}} submitted {{category}} feedback{{pageContext}}. {{message}}');
