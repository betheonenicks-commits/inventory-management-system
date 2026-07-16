package com.iams.storage.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.storage.domain.Attachment;
import com.iams.storage.domain.AttachmentOwnerType;
import com.iams.storage.domain.AttachmentRepository;
import com.iams.storage.infrastructure.ObjectStorageClient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * US-PLAT-02's brokered write/read path. The AC's ordering contract, honored
 * literally: validation happens entirely server-side BEFORE any object-store
 * write, the object commits FIRST, and the DB metadata row commits SECOND.
 * store() is therefore deliberately NOT @Transactional - wrapping external
 * object-store I/O in a DB transaction would invert the contract (row visible
 * before/without the object on rollback races). A failed row insert leaves an
 * orphan object, which is exactly the state the AttachmentJanitor exists to
 * reap; the reverse state - a metadata row pointing at nothing - can never
 * be produced by this ordering.
 */
@Service
public class AttachmentService {

    /**
     * Declared type must be allowlisted AND the magic bytes must agree -
     * trusting the multipart header alone would let any payload masquerade
     * as an image (NFR-SEC-10's "validated before write").
     */
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final AttachmentRepository repository;
    private final ObjectStorageClient storage;
    private final StorageProperties properties;
    private final CurrentUserProvider currentUserProvider;

    public AttachmentService(AttachmentRepository repository, ObjectStorageClient storage,
                             StorageProperties properties, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.storage = storage;
        this.properties = properties;
        this.currentUserProvider = currentUserProvider;
    }

    public Attachment storeImage(AttachmentOwnerType ownerType, UUID ownerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ValidationFailedException.singleField("file", "A file is required");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw ValidationFailedException.singleField("file",
                    "File exceeds the maximum allowed size of " + properties.getMaxSizeBytes() + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw ValidationFailedException.singleField("file",
                    "Content type must be one of " + IMAGE_TYPES + " - got " + contentType);
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading uploaded file", e);
        }
        if (!magicBytesMatch(contentType.toLowerCase(), content)) {
            throw ValidationFailedException.singleField("file",
                    "File content does not match its declared type " + contentType);
        }

        String key = ownerType.name().toLowerCase() + "/" + UUID.randomUUID();
        storage.put(key, content, contentType);

        CurrentUser actor = currentUserProvider.current();
        Attachment attachment = new Attachment();
        attachment.setOwnerType(ownerType);
        attachment.setOwnerId(ownerId);
        attachment.setStorageKey(key);
        attachment.setFileName(sanitizeFileName(file.getOriginalFilename()));
        attachment.setContentType(contentType);
        attachment.setSizeBytes(content.length);
        attachment.setSha256(sha256Hex(content));
        attachment.setUploadedByUserId(actor.id());
        attachment.setUploadedByUsername(actor.username());
        return repository.save(attachment);
    }

    public List<Attachment> listFor(AttachmentOwnerType ownerType, UUID ownerId) {
        return repository.findByOwnerTypeAndOwnerIdOrderByUploadedAtAsc(ownerType, ownerId);
    }

    /**
     * Metadata + bytes, owner-checked: asking for an attachment through the
     * wrong owner is a 404 (existence never leaks across entities), the same
     * discipline every cross-id lookup in this codebase follows.
     */
    public StoredAttachment load(AttachmentOwnerType ownerType, UUID ownerId, UUID attachmentId) {
        Attachment attachment = repository.findByIdAndOwnerTypeAndOwnerId(attachmentId, ownerType, ownerId)
                .orElseThrow(() -> NotFoundException.of("Attachment", attachmentId));
        return new StoredAttachment(attachment, storage.getBytes(attachment.getStorageKey()));
    }

    public record StoredAttachment(Attachment metadata, byte[] content) {
    }

    private static boolean magicBytesMatch(String contentType, byte[] content) {
        return switch (contentType) {
            case "image/jpeg" -> content.length >= 3
                    && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF;
            case "image/png" -> content.length >= 8
                    && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G';
            case "image/webp" -> content.length >= 12
                    && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                    && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
            default -> false;
        };
    }

    private static String sanitizeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "attachment";
        }
        // Strip any path components a hostile client sent; keep the leaf name only.
        String leaf = original.replace('\\', '/');
        leaf = leaf.substring(leaf.lastIndexOf('/') + 1);
        return leaf.length() > 255 ? leaf.substring(leaf.length() - 255) : leaf;
    }

    private static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
