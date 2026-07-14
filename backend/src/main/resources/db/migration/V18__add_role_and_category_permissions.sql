-- US-USR-03: switching @PreAuthorize from hasAnyRole('ADMIN','SUPER_ADMIN') to
-- permission-string checks (roles:read, asset-categories:write) must not change
-- who can do what today. ADMIN's seeded permission set (V15) never needed these
-- two strings because the checks were role-name literals, not permission
-- lookups - so ADMIN needs them added explicitly here to keep GET /roles and
-- asset-category CRUD working for Administrators. SUPER_ADMIN is unaffected
-- (already holds '*'). INVENTORY_MANAGER deliberately does NOT get
-- asset-categories:write - category configuration has always been
-- Administrator-only, unlike day-to-day asset CRUD.
UPDATE role_definition
SET permissions = permissions || '["roles:read", "asset-categories:write"]'::jsonb
WHERE code = 'ADMIN';
