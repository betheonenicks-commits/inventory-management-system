-- EPIC-RPT: reports:read gets its first-ever consumers (the /api/v1/reports
-- endpoints), the exact seeded-but-never-referenced pattern dashboards:read
-- had before V39. V15 already gave it to INVENTORY_MANAGER, DEPARTMENT_HEAD,
-- and VIEWER; the stories also name Administrator (US-RPT-03/14) and
-- Read-only Auditor (US-RPT-08), and a conducting AUDITOR reasonably holds
-- whatever its read-only variant holds. US-RPT-14's security-log report is
-- NOT covered by this permission - it stays on security:read by its own AC.
UPDATE role_definition
SET permissions = permissions || '["reports:read"]'::jsonb
WHERE code IN ('ADMIN', 'AUDITOR', 'READONLY_AUDITOR')
  AND NOT permissions @> '["reports:read"]'::jsonb;
