package com.iams.storage.api;

import com.iams.storage.domain.Attachment;
import java.time.Instant;
import java.util.UUID;

/**
 * Attachment metadata as every owning module's API exposes it. Deliberately
 * carries no storage key or URL - the only way to the bytes is the owning
 * entity's own brokered download endpoint (US-PLAT-02).
 */
public record AttachmentResponse(UUID id, String fileName, String contentType, long sizeBytes, String sha256,
                                  String uploadedByUsername, Instant uploadedAt) {

    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType(), a.getSizeBytes(),
                a.getSha256(), a.getUploadedByUsername(), a.getUploadedAt());
    }
}
