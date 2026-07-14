-- US-LIF-12: "As an Administrator, I want to restore a retired/disposed
-- asset..." - distinct from day-to-day assets:write (Inventory Manager also
-- holds that), the same reasoning V18 used to split asset-categories:write
-- off from assets:write for category configuration.
UPDATE role_definition
SET permissions = permissions || '["assets:restore"]'::jsonb
WHERE code = 'ADMIN' AND NOT permissions @> '["assets:restore"]'::jsonb;
