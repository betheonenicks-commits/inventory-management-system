-- Minimal person record (FR-ORG-04), trimmed to what asset assignment (FR-LIF-04)
-- needs: name, contact, type, org scope. Full DD 1.1 B.1 fields (pseudonym_ref,
-- Active/Departed/Anonymized lifecycle) belong to the later CMP/erasure work and
-- are intentionally absent, not stubbed.
CREATE TABLE person (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name    VARCHAR(120) NOT NULL,
    email        VARCHAR(255),
    person_type  VARCHAR(15)  NOT NULL,
    org_node_id  UUID REFERENCES org_node(id),
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_by   UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by   UUID,
    updated_at   TIMESTAMPTZ
);

CREATE INDEX idx_person_org_node_id ON person (org_node_id);
