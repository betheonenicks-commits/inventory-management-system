-- US-SEC-11: "As an Administrator, I want to search and filter the Security
-- & Access Log" - ADMIN's V15 permission set never included security:read
-- (it didn't exist yet). SUPER_ADMIN is unaffected (already holds '*');
-- IT_SECURITY_OFFICER already has security:read from V15.
UPDATE role_definition
SET permissions = permissions || '["security:read"]'::jsonb
WHERE code = 'ADMIN';
