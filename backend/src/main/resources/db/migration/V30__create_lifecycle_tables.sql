-- EPIC-LIF: asset transfer/disposal requests with configurable approval
-- (US-LIF-05/09/11/12/13), plus approval delegation (US-LIF-15). Both request
-- tables are mutable (version + updated_*), unlike audit_finding/asset_history_event -
-- PENDING -> APPROVED/REJECTED is workflow state, not an append-only fact log;
-- the immutable trail these actions produce lives in asset_history_event instead
-- (TransferService/DisposalService both call AssetHistoryRecorder on approval).

CREATE TABLE asset_transfer_request (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id               UUID         NOT NULL REFERENCES asset(id),
    from_org_node_id       UUID REFERENCES org_node(id),
    to_org_node_id         UUID         NOT NULL REFERENCES org_node(id),
    from_person_id         UUID REFERENCES person(id),
    to_person_id           UUID REFERENCES person(id),
    reason                 VARCHAR(500) NOT NULL,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    nominal_approver_id    UUID         NOT NULL REFERENCES app_user(id),
    effective_approver_id  UUID REFERENCES app_user(id),
    requested_by           UUID         NOT NULL,
    requested_at           TIMESTAMPTZ  NOT NULL,
    decided_by             UUID,
    decided_at             TIMESTAMPTZ,
    rejection_reason       VARCHAR(500),
    version                BIGINT       NOT NULL DEFAULT 0,
    created_by             UUID         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by             UUID,
    updated_at             TIMESTAMPTZ
);

CREATE INDEX idx_asset_transfer_request_asset_id ON asset_transfer_request (asset_id);
CREATE INDEX idx_asset_transfer_request_status ON asset_transfer_request (status);

CREATE TABLE asset_disposal_request (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id                 UUID         NOT NULL REFERENCES asset(id),
    disposal_type            VARCHAR(20)  NOT NULL,
    reason                   VARCHAR(500) NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    nominal_approver_id      UUID         NOT NULL REFERENCES app_user(id),
    effective_approver_id    UUID REFERENCES app_user(id),
    requested_by             UUID         NOT NULL,
    requested_at             TIMESTAMPTZ  NOT NULL,
    decided_by               UUID,
    decided_at               TIMESTAMPTZ,
    rejection_reason         VARCHAR(500),
    disposal_history_event_id UUID REFERENCES asset_history_event(id),
    restored_at              TIMESTAMPTZ,
    restored_by              UUID,
    version                  BIGINT       NOT NULL DEFAULT 0,
    created_by               UUID         NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by               UUID,
    updated_at               TIMESTAMPTZ
);

CREATE INDEX idx_asset_disposal_request_asset_id ON asset_disposal_request (asset_id);
CREATE INDEX idx_asset_disposal_request_status ON asset_disposal_request (status);

-- US-LIF-15: a delegator's approval authority handed to a delegate for a
-- defined window. Plain UUID references (no FK helper table needed beyond
-- app_user), matching Audit.nominal_approver_id's established convention.
CREATE TABLE approval_delegation (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delegator_user_id  UUID         NOT NULL REFERENCES app_user(id),
    delegate_user_id   UUID         NOT NULL REFERENCES app_user(id),
    valid_from         TIMESTAMPTZ  NOT NULL,
    valid_to           TIMESTAMPTZ  NOT NULL,
    active             BOOLEAN      NOT NULL DEFAULT true,
    reason             VARCHAR(500),
    version            BIGINT       NOT NULL DEFAULT 0,
    created_by         UUID         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by         UUID,
    updated_at         TIMESTAMPTZ
);

CREATE INDEX idx_approval_delegation_delegator_user_id ON approval_delegation (delegator_user_id);

-- US-LIF-05/09: Inventory Manager requests transfers/disposals, Department
-- Head approves - same split INVENTORY_MANAGER/DEPARTMENT_HEAD already have
-- via assets:write / approvals:write, no new permission strings needed.
