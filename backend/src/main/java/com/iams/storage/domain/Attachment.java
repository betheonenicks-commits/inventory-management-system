package com.iams.storage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-PLAT-02: metadata for one backend-brokered object-store binary. Rows are
 * immutable once written (evidence discipline, same reasoning as
 * AuditFinding) - there is no update path, and no delete endpoint exists for
 * audit evidence. The row is written only after the object itself committed
 * to the store; see AttachmentService for the ordering contract.
 */
@Getter
@Setter
@Entity
@Table(name = "attachment")
public class Attachment {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, updatable = false, length = 40)
    private AttachmentOwnerType ownerType;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "storage_key", nullable = false, updatable = false, unique = true)
    private String storageKey;

    @Column(name = "file_name", nullable = false, updatable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, updatable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "sha256", nullable = false, updatable = false, length = 64)
    private String sha256;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "uploaded_by_user_id", nullable = false, updatable = false)
    private UUID uploadedByUserId;

    @Column(name = "uploaded_by_username", nullable = false, updatable = false, length = 120)
    private String uploadedByUsername;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }
}
