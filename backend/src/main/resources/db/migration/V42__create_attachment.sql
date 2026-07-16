-- US-PLAT-02: metadata for backend-brokered object-store content. The binary
-- lives in MinIO under storage_key; this row is written only AFTER the object
-- commits (object first, metadata second - a failed row insert leaves an
-- orphan object for the janitor, never a metadata row pointing at nothing).
-- owner_type/owner_id are deliberately polymorphic with no FK: attachments
-- serve audit findings today and asset documents/signatures later, and the
-- owning module enforces existence + authorization at every read/write.
CREATE TABLE attachment (
    id UUID PRIMARY KEY,
    owner_type VARCHAR(40) NOT NULL,
    owner_id UUID NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    uploaded_by_user_id UUID NOT NULL,
    uploaded_by_username VARCHAR(120) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachment_owner ON attachment (owner_type, owner_id);
