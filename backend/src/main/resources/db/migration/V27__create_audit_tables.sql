-- EPIC-AUD: physical audit management (BR-05, the product's stated core
-- differentiator). Five tables: the audit itself (US-AUD-01/13/14), its
-- frozen expected-asset snapshot (US-AUD-04), auditor assignments
-- (US-AUD-02), append-only findings (US-AUD-05/09/10/12/23), and
-- append-only finding corrections (US-AUD-24) - the last two structurally
-- immutable (no version, no updated_* columns), the same pattern
-- asset_history_event and security_event_log already established.

CREATE TABLE audit (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(200) NOT NULL,
    audit_type             VARCHAR(20)  NOT NULL,
    scope_org_node_id      UUID REFERENCES org_node(id),
    scope_category_id      UUID REFERENCES asset_category(id),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    nominal_approver_id    UUID         NOT NULL REFERENCES app_user(id),
    effective_approver_id  UUID REFERENCES app_user(id),
    submitted_by           UUID REFERENCES app_user(id),
    submitted_at           TIMESTAMPTZ,
    signature_name         VARCHAR(200),
    approved_by            UUID REFERENCES app_user(id),
    approved_at            TIMESTAMPTZ,
    last_rejection_reason  VARCHAR(500),
    version                BIGINT       NOT NULL DEFAULT 0,
    created_by             UUID         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by             UUID,
    updated_at             TIMESTAMPTZ,
    CONSTRAINT chk_audit_scope_present CHECK (scope_org_node_id IS NOT NULL OR scope_category_id IS NOT NULL)
);

CREATE INDEX idx_audit_status ON audit (status);
CREATE INDEX idx_audit_scope_org_node_id ON audit (scope_org_node_id);
CREATE INDEX idx_audit_nominal_approver_id ON audit (nominal_approver_id);

-- US-AUD-04: expected-asset snapshot, frozen at creation. An audit scoped by
-- an explicit asset list (rather than org-node/category) still lands here -
-- the snapshot is the source of truth after creation regardless of how the
-- set was originally selected.
CREATE TABLE audit_expected_asset (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_id    UUID NOT NULL REFERENCES audit(id),
    asset_id    UUID NOT NULL REFERENCES asset(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (audit_id, asset_id)
);

CREATE INDEX idx_audit_expected_asset_audit_id ON audit_expected_asset (audit_id);

-- US-AUD-02: auditor assignments, optionally split by sub-scope.
CREATE TABLE audit_assignment (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_id          UUID NOT NULL REFERENCES audit(id),
    auditor_user_id   UUID NOT NULL REFERENCES app_user(id),
    auditor_username  VARCHAR(60) NOT NULL,
    sub_scope         VARCHAR(200),
    active            BOOLEAN NOT NULL DEFAULT true,
    unassigned_at     TIMESTAMPTZ,
    version           BIGINT NOT NULL DEFAULT 0,
    created_by        UUID NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        UUID,
    updated_at        TIMESTAMPTZ
);

CREATE INDEX idx_audit_assignment_audit_id ON audit_assignment (audit_id);
CREATE INDEX idx_audit_assignment_auditor_user_id ON audit_assignment (auditor_user_id);

-- US-AUD-05/09/10/12/23: one row per expected asset's resolution. No
-- version/updated_* columns - see AuditFinding's Javadoc for exactly which
-- fields are (and are not) immutable.
CREATE TABLE audit_finding (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_id                  UUID NOT NULL REFERENCES audit(id),
    asset_id                  UUID NOT NULL REFERENCES asset(id),
    status                    VARCHAR(20) NOT NULL,
    condition                 VARCHAR(20),
    remarks                   VARCHAR(1000),
    verified_by_user_id       UUID REFERENCES app_user(id),
    verified_by_username      VARCHAR(60),
    verified_at               TIMESTAMPTZ NOT NULL,
    device_id                 VARCHAR(100),
    scope_change_disposition  VARCHAR(40),
    UNIQUE (audit_id, asset_id)
);

CREATE INDEX idx_audit_finding_audit_id ON audit_finding (audit_id);
CREATE INDEX idx_audit_finding_status ON audit_finding (status);

-- US-AUD-24: corrections as linked records, never edits to audit_finding.
CREATE TABLE audit_finding_correction (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finding_id      UUID NOT NULL REFERENCES audit_finding(id),
    field_name      VARCHAR(20) NOT NULL,
    old_value       VARCHAR(1000),
    new_value       VARCHAR(1000) NOT NULL,
    actor_id        UUID NOT NULL REFERENCES app_user(id),
    actor_username  VARCHAR(60) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_finding_correction_finding_id ON audit_finding_correction (finding_id);
