-- US-INV-08: the atomic TRANSFER_OUT/TRANSFER_IN pair are mutually-referencing
-- rows created in the same transaction (each is written with the other's
-- pre-assigned id as linked_transaction_id, precisely so neither ever needs a
-- post-insert UPDATE on this otherwise-immutable table - see
-- InventoryStockService.transfer()'s own Javadoc). Postgres checks a plain FK
-- per-statement, so inserting either row first violates the constraint before
-- its counterpart exists - discovered live: the very first transfer attempt
-- against a real database threw exactly this. DEFERRABLE INITIALLY DEFERRED
-- moves the check to transaction commit, by which point both rows exist.

ALTER TABLE inventory_transaction DROP CONSTRAINT inventory_transaction_linked_transaction_id_fkey;
ALTER TABLE inventory_transaction ADD CONSTRAINT inventory_transaction_linked_transaction_id_fkey
    FOREIGN KEY (linked_transaction_id) REFERENCES inventory_transaction(id) DEFERRABLE INITIALLY DEFERRED;
