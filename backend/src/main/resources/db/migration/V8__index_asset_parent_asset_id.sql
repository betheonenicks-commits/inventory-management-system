-- Supports child-lookup queries for US-AST-04 (parent_asset_id column already
-- exists from V6, reserved but unindexed until linking was wired to an endpoint).
CREATE INDEX idx_asset_parent_asset_id ON asset (parent_asset_id) WHERE parent_asset_id IS NOT NULL;
