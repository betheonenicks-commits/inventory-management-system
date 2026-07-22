-- US-AST-04: a parent asset's transfer/disposal must explicitly disposition each
-- child. The per-child decisions are captured at request time and applied at
-- approval, so they persist on the request between those two steps. jsonb map of
-- childAssetId -> disposition (MOVE_WITH_PARENT | DETACH), matching how
-- asset.custom_attributes already stores a per-row map rather than a side table.
ALTER TABLE asset_transfer_request
    ADD COLUMN child_dispositions JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE asset_disposal_request
    ADD COLUMN child_dispositions JSONB NOT NULL DEFAULT '{}'::jsonb;
