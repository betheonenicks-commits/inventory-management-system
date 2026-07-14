-- Closes the FK gap DD 1.1 A.4 specifies for assigned_to_person_id, now that
-- person exists (V9). Reserved, unenforced since V6; FR-LIF-04.
ALTER TABLE asset
    ADD CONSTRAINT fk_asset_assigned_to_person FOREIGN KEY (assigned_to_person_id) REFERENCES person(id);

CREATE INDEX idx_asset_assigned_to_person_id ON asset (assigned_to_person_id) WHERE assigned_to_person_id IS NOT NULL;
