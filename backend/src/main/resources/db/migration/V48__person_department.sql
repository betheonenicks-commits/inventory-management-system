-- US-ORG-03 (AC-ORG-03-H): a person can reference a department/cost centre as
-- its own dimension, independent of physical location - the same wiring
-- V47 added for assets. Nullable FK; a person without a department is normal.
ALTER TABLE person ADD COLUMN department_id UUID REFERENCES department(id);
