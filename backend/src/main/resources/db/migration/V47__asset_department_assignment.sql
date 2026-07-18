-- US-LIF-04 (department half): an asset's custodian can be a Department, not
-- just a Person. Custodian is exclusive - a Person XOR a Department - enforced
-- in AssetAssignmentService, not by the schema (both columns nullable). The
-- Department dimension (FR-ORG-03, V-org) already existed; this is the wiring
-- into Asset that was deferred when Department shipped standalone.
ALTER TABLE asset ADD COLUMN assigned_to_department_id UUID REFERENCES department(id);
