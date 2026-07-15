package com.iams.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.asset.application.AssetHistoryRecorder;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingReconciliation;
import com.iams.audit.domain.AuditFindingReconciliationRepository;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.FindingStatus;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditReconciliationServiceTest {

    @Mock private AuditFindingRepository findingRepository;
    @Mock private AuditFindingReconciliationRepository reconciliationRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private AssetStatusDefRepository statusDefRepository;
    @Mock private AssetHistoryRecorder historyRecorder;
    @Mock private CurrentUserProvider currentUserProvider;

    private AuditReconciliationService service;
    private UUID auditId;
    private UUID findingId;
    private UUID actorId;
    private Audit audit;
    private Asset asset;
    private AuditFinding finding;

    @BeforeEach
    void setUp() {
        service = new AuditReconciliationService(findingRepository, reconciliationRepository, assetRepository,
                statusDefRepository, historyRecorder, currentUserProvider);
        auditId = UUID.randomUUID();
        findingId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        audit = new Audit();
        audit.setId(auditId);
        asset = new Asset();
        asset.setId(UUID.randomUUID());
        AssetStatusDef missingStatus = new AssetStatusDef();
        missingStatus.setCode("MISSING");
        asset.setStatus(missingStatus);

        finding = new AuditFinding();
        finding.setId(findingId);
        finding.setAudit(audit);
        finding.setAsset(asset);
        finding.setStatus(FindingStatus.MISSING);
        finding.setPreviousStatusCode("IN_USE");

        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(actorId, "invmgr", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void reconcile_createsLinkedRecord_andRevertsAssetToPreviousStatus() {
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));
        when(reconciliationRepository.findByFindingId(findingId)).thenReturn(Optional.empty());
        AssetStatusDef inUse = new AssetStatusDef();
        inUse.setCode("IN_USE");
        when(statusDefRepository.findByCode("IN_USE")).thenReturn(Optional.of(inUse));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(reconciliationRepository.save(org.mockito.ArgumentMatchers.any(AuditFindingReconciliation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditFindingReconciliation result = service.reconcile(auditId, findingId, "Found in Room 310");

        assertThat(result.getFoundLocationNote()).isEqualTo("Found in Room 310");
        assertThat(result.getFinding()).isSameAs(finding);
        assertThat(result.getReconciledByUserId()).isEqualTo(actorId);
        assertThat(asset.getStatus()).isSameAs(inUse);
        // AC-AUD-21-X: the original finding itself is never mutated - still MISSING, untouched.
        assertThat(finding.getStatus()).isEqualTo(FindingStatus.MISSING);
    }

    @Test
    void reconcile_fallsBackToInStorage_whenPreviousStatusUnknown() {
        finding.setPreviousStatusCode(null);
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));
        when(reconciliationRepository.findByFindingId(findingId)).thenReturn(Optional.empty());
        AssetStatusDef inStorage = new AssetStatusDef();
        inStorage.setCode("IN_STORAGE");
        when(statusDefRepository.findByCode("IN_STORAGE")).thenReturn(Optional.of(inStorage));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(reconciliationRepository.save(org.mockito.ArgumentMatchers.any(AuditFindingReconciliation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.reconcile(auditId, findingId, "Found in the loading dock");

        assertThat(asset.getStatus()).isSameAs(inStorage);
    }

    @Test
    void reconcile_rejectsBlankLocationNote() {
        assertThatThrownBy(() -> service.reconcile(auditId, findingId, "  "))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void reconcile_rejectsFindingNotMissing() {
        finding.setStatus(FindingStatus.VERIFIED);
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));

        assertThatThrownBy(() -> service.reconcile(auditId, findingId, "Found it"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Missing");
    }

    @Test
    void reconcile_rejectsAlreadyReconciledFinding() {
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));
        when(reconciliationRepository.findByFindingId(findingId))
                .thenReturn(Optional.of(new AuditFindingReconciliation()));

        assertThatThrownBy(() -> service.reconcile(auditId, findingId, "Found it again"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been reconciled");
    }

    @Test
    void reconcile_rejectsFindingFromDifferentAudit() {
        UUID otherAuditId = UUID.randomUUID();
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));

        assertThatThrownBy(() -> service.reconcile(otherAuditId, findingId, "Found it"))
                .isInstanceOf(NotFoundException.class);
    }
}
