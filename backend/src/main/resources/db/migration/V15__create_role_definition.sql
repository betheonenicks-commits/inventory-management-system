-- Role catalog (FR-USR-01, FR-USR-02, US-USR-01/02/07). Roles are flat and
-- non-inheriting (US-USR-07): a user needing multiple capabilities gets
-- multiple role rows via user_role_assignment, never a role that implies
-- another. permissions is a JSON array of permission-code strings, checked
-- the same way asset.custom_attributes is - a jsonb column, not a normalized
-- permission table, since nothing yet needs to query "which roles grant
-- permission X" independent of a specific role (only per-role reads).
--
-- is_sensitive = true means only a SUPER_ADMIN actor may assign this role to
-- someone (US-USR-01's exception AC: "security-sensitive roles are
-- Super-Admin-only to assign"). is_assignable_to_humans = false is reserved
-- for INTEGRATION_SERVICE (FR-SEC-14: "non-assignable to humans") - enforced
-- here even though the service-account issuance flow itself (US-SEC-14)
-- doesn't exist yet, because the invariant is cheap to guard now and wrong
-- to leave open in the meantime.
CREATE TABLE role_definition (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                    VARCHAR(40)  NOT NULL UNIQUE,
    name                    VARCHAR(100) NOT NULL,
    description             VARCHAR(500),
    is_system               BOOLEAN      NOT NULL DEFAULT false,
    is_sensitive            BOOLEAN      NOT NULL DEFAULT false,
    is_assignable_to_humans BOOLEAN      NOT NULL DEFAULT true,
    permissions             JSONB        NOT NULL DEFAULT '[]'::jsonb,
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_by              UUID         NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by              UUID,
    updated_at              TIMESTAMPTZ
);

-- The nine FR-USR-01 default roles, plus the two system-provided custom roles
-- (IT Security Officer, Compliance Officer) it names, plus the non-human
-- Integration Service role (FR-SEC-14). All seeded as is_system = true: they
-- ship predefined and (per FR-USR-01) are not meant to be deleted, though
-- US-USR-02 custom-role creation is unaffected by that flag.
INSERT INTO role_definition (code, name, description, is_system, is_sensitive, is_assignable_to_humans, permissions, created_by)
VALUES
    ('SUPER_ADMIN', 'Super Administrator',
        'Full system access, including security-sensitive role assignment and break-glass access.',
        true, true, true, '["*"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    -- Code is 'ADMIN', not 'ADMINISTRATOR': every existing @PreAuthorize check
    -- across the asset and org controllers (13 of them, written before this
    -- role table existed) already hardcodes hasAnyRole('ADMIN','SUPER_ADMIN').
    -- Matching that existing convention here, rather than the other way
    -- around, is what makes those checks start actually enforcing something.
    ('ADMIN', 'Administrator',
        'User provisioning/offboarding, day-to-day configuration, cross-department coordination. Cannot alter security policy or retention rules.',
        true, false, true,
        '["users:read","users:write","org:read","org:write","assets:read","assets:write"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('SYSTEM_OPERATOR', 'System Operator',
        'Technical configuration only (backups, LDAP, system health) - no business or personal data access beyond operational need (FR-USR-05).',
        true, false, true,
        '["system:read","system:write"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('INVENTORY_MANAGER', 'Inventory Manager',
        'Registers and maintains assets, runs reports, manages inventory and procurement.',
        true, false, true,
        '["assets:read","assets:write","inventory:read","inventory:write","reports:read"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('AUDITOR', 'Auditor',
        'Conducts physical audits: scanning, findings, submission.',
        true, false, true,
        '["assets:read","audits:read","audits:write"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('READONLY_AUDITOR', 'Read-only Auditor',
        'Reviews audit history and certificates without conducting audits.',
        true, false, true,
        '["assets:read","audits:read"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('DEPARTMENT_HEAD', 'Department Head',
        'Approves transfers/disposals/audits for their scope; views department-scoped dashboards and reports.',
        true, false, true,
        '["assets:read","audits:read","approvals:read","approvals:write","reports:read"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('EMPLOYEE_VOLUNTEER', 'Employee/Volunteer',
        'Views assets assigned to them and receives related notifications. No administrative access.',
        true, false, true,
        '["assets:read:own"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('VIEWER', 'Viewer',
        'Read-only access to dashboards and reports within permitted scope (e.g. board member, finance officer).',
        true, false, true,
        '["dashboards:read","reports:read"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('IT_SECURITY_OFFICER', 'IT Security Officer',
        'System-provided custom role (FR-USR-01/FR-USR-02): security log access, SoD waiver sign-off, break-glass notification.',
        true, true, true,
        '["security:read","security:write","sod-waivers:write"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('COMPLIANCE_OFFICER', 'Compliance Officer',
        'System-provided custom role (FR-USR-01/FR-USR-02): retention policy, legal holds, data-subject export/erasure.',
        true, true, true,
        '["compliance:read","compliance:write"]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad'),
    ('INTEGRATION_SERVICE', 'Integration Service',
        'Non-human role for scoped service-account credentials (FR-SEC-14). Never assignable to a human user.',
        true, true, false,
        '[]'::jsonb,
        '00000000-0000-0000-0000-0000000000ad');
