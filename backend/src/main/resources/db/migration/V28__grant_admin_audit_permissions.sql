-- US-AUD-24: the story's own AC names "the Auditor assigned to the audit or
-- an Administrator" as who may submit a finding correction - ADMIN's V15
-- permission set predates EPIC-AUD and never included audits:*. SUPER_ADMIN
-- is unaffected (already holds '*'); AUDITOR/READONLY_AUDITOR/DEPARTMENT_HEAD
-- already have audits:read/audits:write from V15.
UPDATE role_definition
SET permissions = permissions || '["audits:read","audits:write"]'::jsonb
WHERE code = 'ADMIN';
