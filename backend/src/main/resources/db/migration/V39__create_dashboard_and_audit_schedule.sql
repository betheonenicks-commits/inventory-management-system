-- EPIC-DSH (Dashboard, BR-08). Three things:
--
-- 1. dashboard_preference: per-user KPI tile selection (US-DSH-06). One row
--    per user, tiles stored as a jsonb array of DashboardTile enum names -
--    "no row yet" is the "sensible default set" state, not an empty array
--    (an empty array is a deliberate, saved choice to show nothing).
--
-- 2. audit.scheduled_date: US-DSH-05's audit-calendar AC ("audits scheduled
--    in the next 30 days ... on its scheduled date") needs a date to plot,
--    and Audit never had a scheduling concept - audits were created straight
--    into IN_PROGRESS. Nullable and optional at creation: the same small,
--    honest cross-epic extension pattern as purchase_order.vendor_id (added
--    by EPIC-INV) - serving a real AC, not speculative scope.
--
-- 3. dashboards:read grants: V15 seeded this permission onto VIEWER ("read-only
--    access to dashboards and reports" is that role's entire reason to exist)
--    but no endpoint has ever used it - the same seeded-but-never-referenced
--    class as the MISSING asset status (fixed in V35's session). The dashboard
--    stories are written for Inventory Manager (01/03/04), Department Head
--    (02/05/07), and Viewer (06), and DEPARTMENT_HEAD's own V15 description
--    already says "views department-scoped dashboards" - so every role whose
--    story or description names dashboards gets the permission. EMPLOYEE_VOLUNTEER
--    deliberately does not (no DSH story names them, and their V15 description
--    says "no administrative access").

CREATE TABLE dashboard_preference (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL UNIQUE REFERENCES app_user(id),
    tiles       JSONB       NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_by  UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID,
    updated_at  TIMESTAMPTZ
);

ALTER TABLE audit ADD COLUMN scheduled_date DATE;

UPDATE role_definition
SET permissions = permissions || '["dashboards:read"]'::jsonb
WHERE code IN ('ADMIN', 'INVENTORY_MANAGER', 'AUDITOR', 'READONLY_AUDITOR', 'DEPARTMENT_HEAD')
  AND NOT permissions @> '["dashboards:read"]'::jsonb;
