-- US-AUD-21: reconciling a previously-Missing asset outside an active audit,
-- as a new linked record - never an edit to audit_finding (mirrors
-- audit_finding_correction's "new row, not an edit" discipline, but links to
-- a whole finding rather than correcting one field of it).
--
-- previous_status_code lets reconciliation revert the asset to exactly where
-- it was before being classified Missing (mirrors repair_event's own
-- previous-status capture from EPIC-LIF), rather than a fixed fallback.
-- Populated only for MISSING findings, going forward from this migration -
-- AuditWorkflowService.classifyMissing() now also sets the asset's own
-- status to the long-seeded-but-never-used MISSING AssetStatusDef (V3) at
-- the same moment, which is what reconciliation's "updates the asset's
-- status" (AC-AUD-21-H) actually reverts.

ALTER TABLE audit_finding ADD COLUMN previous_status_code VARCHAR(50);

CREATE TABLE audit_finding_reconciliation (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finding_id              UUID NOT NULL REFERENCES audit_finding(id),
    found_location_note     VARCHAR(500) NOT NULL,
    reconciled_by_user_id   UUID NOT NULL REFERENCES app_user(id),
    reconciled_by_username  VARCHAR(60) NOT NULL,
    reconciled_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (finding_id)
);

CREATE INDEX idx_audit_finding_reconciliation_finding_id ON audit_finding_reconciliation (finding_id);
