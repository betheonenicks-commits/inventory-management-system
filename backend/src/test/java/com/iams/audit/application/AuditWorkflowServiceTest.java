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
import com.iams.audit.domain.AuditExpectedAsset;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.FindingStatus;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.lifecycle.application.ApprovalRoutingService;
import com.iams.lifecycle.application.LifecycleProperties;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.SodWaiver;
import com.iams.usr.domain.SodWaiverRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuditWorkflowServiceTest {

    @Mock private AuditRepository auditRepository;
    @Mock private AuditExpectedAssetRepository expectedAssetRepository;
    @Mock private AuditFindingRepository findingRepository;
    @Mock private SodWaiverRepository sodWaiverRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AssetRepository assetRepository;
    @Mock private AssetStatusDefRepository statusDefRepository;
    @Mock private AssetHistoryRecorder historyRecorder;
    @Mock private ApprovalRoutingService routingService;

    private AuditWorkflowService service;
    private UUID actorId;
    private UUID auditId;
    private Audit audit;
    private AppUser actorUser;

    @BeforeEach
    void setUp() {
        service = new AuditWorkflowService(auditRepository, expectedAssetRepository, findingRepository,
                sodWaiverRepository, appUserRepository, passwordEncoder, currentUserProvider, assetRepository,
                statusDefRepository, historyRecorder, routingService, new LifecycleProperties());
        actorId = UUID.randomUUID();
        auditId = UUID.randomUUID();
        audit = new Audit();
        audit.setId(auditId);
        audit.setStatus(AuditStatus.IN_PROGRESS);
        audit.setNominalApproverId(UUID.randomUUID());
        actorUser = new AppUser();
        actorUser.setId(actorId);
        actorUser.setDisplayName("Auditor One");
        actorUser.setPasswordHash("hashed");
        org.mockito.Mockito.lenient().when(currentUserProvider.current()).thenReturn(new CurrentUser(actorId, "auditor1", Set.of("AUDITOR")));
    }

    @Test
    void submit_succeeds_signsAndMovesToPendingApproval() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(findingRepository.existsByAuditIdAndStatusAndScopeChangeDispositionIsNull(auditId, FindingStatus.SCOPE_CHANGED))
                .thenReturn(false);
        when(appUserRepository.findById(actorId)).thenReturn(Optional.of(actorUser));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);
        when(expectedAssetRepository.findByAuditIdWithAsset(auditId)).thenReturn(List.of());
        when(auditRepository.saveAndFlush(audit)).thenReturn(audit);

        Audit result = service.submit(auditId, "correct-password", "Auditor One");

        assertThat(result.getStatus()).isEqualTo(AuditStatus.PENDING_APPROVAL);
        assertThat(result.getSubmittedBy()).isEqualTo(actorId);
        assertThat(result.getSignatureName()).isEqualTo("Auditor One");
        assertThat(result.getEffectiveApproverId()).isEqualTo(audit.getNominalApproverId());
    }

    @Test
    void submit_leavesAuditUnsubmitted_whenPasswordWrong() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(findingRepository.existsByAuditIdAndStatusAndScopeChangeDispositionIsNull(auditId, FindingStatus.SCOPE_CHANGED))
                .thenReturn(false);
        when(appUserRepository.findById(actorId)).thenReturn(Optional.of(actorUser));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.submit(auditId, "wrong", null))
                .isInstanceOf(ValidationFailedException.class);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.IN_PROGRESS);
        assertThat(audit.getSubmittedAt()).isNull();
    }

    @Test
    void submit_blocksSelfApproval_whenNoActiveWaiver() {
        audit.setNominalApproverId(actorId);
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(findingRepository.existsByAuditIdAndStatusAndScopeChangeDispositionIsNull(auditId, FindingStatus.SCOPE_CHANGED))
                .thenReturn(false);
        when(appUserRepository.findById(actorId)).thenReturn(Optional.of(actorUser));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);
        when(sodWaiverRepository.findActiveByScopeOrderByCreatedAtDesc("AUDIT_APPROVAL")).thenReturn(List.of());

        assertThatThrownBy(() -> service.submit(auditId, "correct-password", null))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("cannot submit it yourself");
    }

    @Test
    void submit_reroutesToWaiverSigner_whenActiveWaiverCoversSelfApproval() {
        audit.setNominalApproverId(actorId);
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(findingRepository.existsByAuditIdAndStatusAndScopeChangeDispositionIsNull(auditId, FindingStatus.SCOPE_CHANGED))
                .thenReturn(false);
        when(appUserRepository.findById(actorId)).thenReturn(Optional.of(actorUser));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        UUID alternateApproverId = UUID.randomUUID();
        AppUser alternate = new AppUser();
        alternate.setId(alternateApproverId);
        SodWaiver waiver = new SodWaiver();
        waiver.setSignedOffBy(alternate);
        when(sodWaiverRepository.findActiveByScopeOrderByCreatedAtDesc("AUDIT_APPROVAL")).thenReturn(List.of(waiver));
        when(expectedAssetRepository.findByAuditIdWithAsset(auditId)).thenReturn(List.of());
        when(auditRepository.saveAndFlush(audit)).thenReturn(audit);

        Audit result = service.submit(auditId, "correct-password", null);

        assertThat(result.getEffectiveApproverId()).isEqualTo(alternateApproverId);
        assertThat(result.getStatus()).isEqualTo(AuditStatus.PENDING_APPROVAL);
    }

    @Test
    void submit_blocksOnOpenScopeChangeDisposition() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(findingRepository.existsByAuditIdAndStatusAndScopeChangeDispositionIsNull(auditId, FindingStatus.SCOPE_CHANGED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submit(auditId, "any-password", null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void submit_classifiesUnverifiedExpectedAssetsAsMissing() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(findingRepository.existsByAuditIdAndStatusAndScopeChangeDispositionIsNull(auditId, FindingStatus.SCOPE_CHANGED))
                .thenReturn(false);
        when(appUserRepository.findById(actorId)).thenReturn(Optional.of(actorUser));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        Asset scannedAsset = new Asset();
        scannedAsset.setId(UUID.randomUUID());
        Asset neverScannedAsset = new Asset();
        neverScannedAsset.setId(UUID.randomUUID());
        AssetStatusDef inStorage = new AssetStatusDef();
        inStorage.setCode("IN_STORAGE");
        neverScannedAsset.setStatus(inStorage);
        AssetStatusDef missingStatus = new AssetStatusDef();
        missingStatus.setCode("MISSING");

        AuditExpectedAsset row1 = new AuditExpectedAsset();
        row1.setAsset(scannedAsset);
        AuditExpectedAsset row2 = new AuditExpectedAsset();
        row2.setAsset(neverScannedAsset);
        when(expectedAssetRepository.findByAuditIdWithAsset(auditId)).thenReturn(List.of(row1, row2));
        when(findingRepository.findByAuditIdAndAssetId(auditId, scannedAsset.getId()))
                .thenReturn(Optional.of(new AuditFinding()));
        when(findingRepository.findByAuditIdAndAssetId(auditId, neverScannedAsset.getId()))
                .thenReturn(Optional.empty());
        when(statusDefRepository.findByCode("MISSING")).thenReturn(Optional.of(missingStatus));
        when(assetRepository.saveAndFlush(neverScannedAsset)).thenReturn(neverScannedAsset);
        when(auditRepository.saveAndFlush(audit)).thenReturn(audit);

        service.submit(auditId, "correct-password", null);

        org.mockito.ArgumentCaptor<AuditFinding> captor = org.mockito.ArgumentCaptor.forClass(AuditFinding.class);
        org.mockito.Mockito.verify(findingRepository).save(captor.capture());
        assertThat(captor.getValue().getAsset()).isSameAs(neverScannedAsset);
        assertThat(captor.getValue().getStatus()).isEqualTo(FindingStatus.MISSING);
        assertThat(captor.getValue().getVerifiedByUserId()).isNull();
        // US-AUD-09/21: the finding captures the asset's real prior status, and the asset itself flips to MISSING.
        assertThat(captor.getValue().getPreviousStatusCode()).isEqualTo("IN_STORAGE");
        assertThat(neverScannedAsset.getStatus()).isSameAs(missingStatus);
    }

    @Test
    void approve_closesAudit_whenActedByRoutedApprover() {
        UUID approverId = UUID.randomUUID();
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        audit.setEffectiveApproverId(approverId);
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(auditRepository.saveAndFlush(audit)).thenReturn(audit);

        Audit result = service.approve(auditId);

        assertThat(result.getStatus()).isEqualTo(AuditStatus.CLOSED);
        assertThat(result.getApprovedBy()).isEqualTo(approverId);
    }

    @Test
    void approve_rejectsAnyoneOtherThanRoutedApprover() {
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        audit.setEffectiveApproverId(UUID.randomUUID());
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));

        assertThatThrownBy(() -> service.approve(auditId)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void reject_resetsAuditBackToInProgress() {
        UUID approverId = UUID.randomUUID();
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        audit.setEffectiveApproverId(approverId);
        audit.setSubmittedAt(java.time.Instant.now());
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(auditRepository.saveAndFlush(audit)).thenReturn(audit);

        Audit result = service.reject(auditId, "Missing findings need more detail");

        assertThat(result.getStatus()).isEqualTo(AuditStatus.IN_PROGRESS);
        assertThat(result.getSubmittedAt()).isNull();
        assertThat(result.getLastRejectionReason()).isEqualTo("Missing findings need more detail");
    }

    @Test
    void reject_undoesSystemClassifiedMissingFindings_soAssetCanBeRescanned() {
        UUID approverId = UUID.randomUUID();
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        audit.setEffectiveApproverId(approverId);
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(auditRepository.saveAndFlush(audit)).thenReturn(audit);

        AuditFinding systemMissing = new AuditFinding();
        systemMissing.setStatus(FindingStatus.MISSING);
        when(findingRepository.findByAuditIdAndStatusAndVerifiedByUserIdIsNull(auditId, FindingStatus.MISSING))
                .thenReturn(List.of(systemMissing));

        service.reject(auditId, "reason");

        org.mockito.Mockito.verify(findingRepository).deleteAll(List.of(systemMissing));
    }

    @Test
    void reject_revertsAssetStatus_forFindingsThatHadFlippedTheAssetToMissing() {
        UUID approverId = UUID.randomUUID();
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        audit.setEffectiveApproverId(approverId);
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(auditRepository.saveAndFlush(audit)).thenReturn(audit);

        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        AuditFinding systemMissing = new AuditFinding();
        systemMissing.setStatus(FindingStatus.MISSING);
        systemMissing.setAsset(asset);
        systemMissing.setPreviousStatusCode("IN_USE");
        when(findingRepository.findByAuditIdAndStatusAndVerifiedByUserIdIsNull(auditId, FindingStatus.MISSING))
                .thenReturn(List.of(systemMissing));
        AssetStatusDef inUse = new AssetStatusDef();
        inUse.setCode("IN_USE");
        when(statusDefRepository.findByCode("IN_USE")).thenReturn(Optional.of(inUse));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);

        service.reject(auditId, "reason");

        assertThat(asset.getStatus()).isSameAs(inUse);
    }

    @Test
    void escalate_throwsConflict_whenBelowThreshold() {
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        audit.setSubmittedAt(java.time.Instant.now());
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));

        assertThatThrownBy(() -> service.escalate(auditId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void escalate_reroutesToTarget_whenThresholdReached() {
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        audit.setSubmittedAt(java.time.Instant.now().minus(java.time.Duration.ofHours(73)));
        UUID currentApprover = audit.getNominalApproverId();
        UUID escalationTarget = UUID.randomUUID();
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(routingService.resolveEscalationTarget(currentApprover)).thenReturn(escalationTarget);
        when(auditRepository.saveAndFlush(audit)).thenReturn(audit);

        Audit result = service.escalate(auditId);

        assertThat(result.getEffectiveApproverId()).isEqualTo(escalationTarget);
    }
}
