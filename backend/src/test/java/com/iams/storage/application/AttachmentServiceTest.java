package com.iams.storage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.storage.domain.Attachment;
import com.iams.storage.domain.AttachmentOwnerType;
import com.iams.storage.domain.AttachmentRepository;
import com.iams.storage.infrastructure.ObjectStorageClient;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 9, 9};

    @Mock private AttachmentRepository repository;
    @Mock private ObjectStorageClient storage;
    @Mock private CurrentUserProvider currentUserProvider;

    private StorageProperties properties;
    private AttachmentService service;
    private UUID ownerId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        service = new AttachmentService(repository, storage, properties, currentUserProvider);
        ownerId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private void stubActor() {
        when(currentUserProvider.current())
                .thenReturn(new CurrentUser(userId, "auditor1", Set.of("AUDITOR"), Set.of("audits:write")));
        when(repository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void storeImage_putsObjectBeforeSavingMetadataRow() {
        stubActor();
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);

        Attachment saved = service.storeImage(AttachmentOwnerType.AUDIT_FINDING, ownerId, file);

        // The AC's ordering contract: object first, row second.
        InOrder order = inOrder(storage, repository);
        order.verify(storage).put(anyString(), any(byte[].class), anyString());
        order.verify(repository).save(any(Attachment.class));

        assertThat(saved.getOwnerType()).isEqualTo(AttachmentOwnerType.AUDIT_FINDING);
        assertThat(saved.getOwnerId()).isEqualTo(ownerId);
        assertThat(saved.getFileName()).isEqualTo("photo.png");
        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getSizeBytes()).isEqualTo(PNG_BYTES.length);
        assertThat(saved.getSha256()).hasSize(64).matches("[0-9a-f]+");
        assertThat(saved.getStorageKey()).startsWith("audit_finding/");
        assertThat(saved.getUploadedByUserId()).isEqualTo(userId);
        assertThat(saved.getUploadedByUsername()).isEqualTo("auditor1");
    }

    @Test
    void storeImage_rejectsEmptyFileBeforeAnyStorageWrite() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.storeImage(AttachmentOwnerType.AUDIT_FINDING, ownerId, file))
                .isInstanceOf(ValidationFailedException.class);
        verify(storage, never()).put(anyString(), any(byte[].class), anyString());
    }

    @Test
    void storeImage_rejectsOversizedFileBeforeAnyStorageWrite() {
        properties.setMaxSizeBytes(4);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);

        assertThatThrownBy(() -> service.storeImage(AttachmentOwnerType.AUDIT_FINDING, ownerId, file))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("size");
        verify(storage, never()).put(anyString(), any(byte[].class), anyString());
    }

    @Test
    void storeImage_rejectsDisallowedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", PNG_BYTES);

        assertThatThrownBy(() -> service.storeImage(AttachmentOwnerType.AUDIT_FINDING, ownerId, file))
                .isInstanceOf(ValidationFailedException.class);
        verify(storage, never()).put(anyString(), any(byte[].class), anyString());
    }

    @Test
    void storeImage_rejectsContentThatContradictsDeclaredType() {
        // Declared png, actual jpeg bytes - the multipart header alone is never trusted.
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", JPEG_BYTES);

        assertThatThrownBy(() -> service.storeImage(AttachmentOwnerType.AUDIT_FINDING, ownerId, file))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("does not match");
        verify(storage, never()).put(anyString(), any(byte[].class), anyString());
    }

    @Test
    void storeImage_stripsPathComponentsFromHostileFileName() {
        stubActor();
        MockMultipartFile file =
                new MockMultipartFile("file", "..\\..\\secrets\\photo.png", "image/png", PNG_BYTES);

        Attachment saved = service.storeImage(AttachmentOwnerType.AUDIT_FINDING, ownerId, file);

        assertThat(saved.getFileName()).isEqualTo("photo.png");
    }

    @Test
    void load_isNotFoundWhenAttachmentBelongsToAnotherOwner() {
        UUID attachmentId = UUID.randomUUID();
        when(repository.findByIdAndOwnerTypeAndOwnerId(attachmentId, AttachmentOwnerType.AUDIT_FINDING, ownerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.load(AttachmentOwnerType.AUDIT_FINDING, ownerId, attachmentId))
                .isInstanceOf(NotFoundException.class);
    }
}
