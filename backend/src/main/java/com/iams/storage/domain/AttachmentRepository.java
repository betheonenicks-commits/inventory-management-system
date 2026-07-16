package com.iams.storage.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByOwnerTypeAndOwnerIdOrderByUploadedAtAsc(AttachmentOwnerType ownerType, UUID ownerId);

    Optional<Attachment> findByIdAndOwnerTypeAndOwnerId(UUID id, AttachmentOwnerType ownerType, UUID ownerId);

    boolean existsByStorageKey(String storageKey);
}
