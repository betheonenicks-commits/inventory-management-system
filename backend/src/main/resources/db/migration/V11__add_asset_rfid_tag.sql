-- Reserved RFID identifier field (FR-AST-12, US-AST-12). Nullable/unused
-- until R3 hardware exists - no abstraction layer here, that's US-SCN-06.
ALTER TABLE asset ADD COLUMN rfid_tag_id VARCHAR(100) UNIQUE;
