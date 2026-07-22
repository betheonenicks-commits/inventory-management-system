package com.iams.lifecycle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.asset.application.AssetAssignmentService;
import com.iams.asset.application.AssetHistoryRecorder;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetRepository;
import com.iams.audit.application.AuditScopeChangeService;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.application.LegalHoldService;
import com.iams.lifecycle.domain.AssetTransferRequest;
import com.iams.lifecycle.domain.AssetTransferRequestRepository;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.SodWaiverRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private AssetTransferRequestRepository transferRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private OrgNodeRepository orgNodeRepository;
    @Mock private AssetHistoryRecorder historyRecorder;
    @Mock private AssetAssignmentService assignmentService;
    @Mock private ApprovalRoutingService routingService;
    @Mock private AuditScopeChangeService auditScopeChangeService;
    @Mock private AppUserRepository appUserRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OrgScopeGuard scopeGuard;
    @Mock private LifecycleProperties lifecycleProperties;
    @Mock private LegalHoldService legalHoldService;
    @Mock private SodWaiverRepository sodWaiverRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private TransferService service;
    private UUID actorId;
    private UUID assetId;
    private UUID toOrgNodeId;
    private UUID approverId;
    private Asset asset;
    private OrgNode fromNode;
    private OrgNode toNode;

    @BeforeEach
    void setUp() {
        service = new TransferService(transferRepository, assetRepository, orgNodeRepository, historyRecorder,
                assignmentService, routingService, auditScopeChangeService, appUserRepository, currentUserProvider,
                scopeGuard, lifecycleProperties, legalHoldService, eventPublisher, sodWaiverRepository);
        actorId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        toOrgNodeId = UUID.randomUUID();
        approverId = UUID.randomUUID();

        fromNode = new OrgNode();
        fromNode.setId(UUID.randomUUID());
        fromNode.setCode("ROOM-A");
        toNode = new OrgNode();
        toNode.setId(toOrgNodeId);
        toNode.setCode("ROOM-B");

        asset = new Asset();
        asset.setId(assetId);
        asset.setOrgNode(fromNode);

        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(actorId, "manager1", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void create_succeeds_withValidReasonAndApprover() {
        when(assetRepository.findByIdWithAssociations(assetId)).thenReturn(Optional.of(asset));
        when(orgNodeRepository.findById(toOrgNodeId)).thenReturn(Optional.of(toNode));
        when(appUserRepository.existsById(approverId)).thenReturn(true);
        when(transferRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        AssetTransferRequest result = service.create(new TransferCreateCommand(assetId, toOrgNodeId, null, "Relocation", approverId, java.util.Map.of()));

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.PENDING);
        assertThat(result.getFromOrgNode()).isEqualTo(fromNode);
        assertThat(result.getToOrgNode()).isEqualTo(toNode);
        assertThat(result.getRequestedBy()).isEqualTo(actorId);
    }

    @Test
    void create_rejectsBlankReason() {
        assertThatThrownBy(() -> service.create(new TransferCreateCommand(assetId, toOrgNodeId, null, " ", approverId, java.util.Map.of())))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsUnknownApprover() {
        when(assetRepository.findByIdWithAssociations(assetId)).thenReturn(Optional.of(asset));
        when(orgNodeRepository.findById(toOrgNodeId)).thenReturn(Optional.of(toNode));
        when(appUserRepository.existsById(approverId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new TransferCreateCommand(assetId, toOrgNodeId, null, "Relocation", approverId, java.util.Map.of())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_blocksWhenAParentHasAnUndispositionedChild() {
        // US-AST-04: the asset has a child, but no disposition was supplied for it - the request is blocked.
        Asset child = new Asset();
        child.setId(UUID.randomUUID());
        child.setAssetNumber("AST-CHILD-1");
        when(assetRepository.findByIdWithAssociations(assetId)).thenReturn(Optional.of(asset));
        when(orgNodeRepository.findById(toOrgNodeId)).thenReturn(Optional.of(toNode));
        when(appUserRepository.existsById(approverId)).thenReturn(true);
        when(assetRepository.findByParentAssetIdWithAssociationsOrderByCreatedAtAsc(assetId))
                .thenReturn(java.util.List.of(child));

        assertThatThrownBy(() -> service.create(
                new TransferCreateCommand(assetId, toOrgNodeId, null, "Relocation", approverId, java.util.Map.of())))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("AST-CHILD-1");
    }

    @Test
    void create_succeeds_whenEveryChildIsDispositioned() {
        Asset child = new Asset();
        UUID childId = UUID.randomUUID();
        child.setId(childId);
        child.setAssetNumber("AST-CHILD-1");
        when(assetRepository.findByIdWithAssociations(assetId)).thenReturn(Optional.of(asset));
        when(orgNodeRepository.findById(toOrgNodeId)).thenReturn(Optional.of(toNode));
        when(appUserRepository.existsById(approverId)).thenReturn(true);
        when(assetRepository.findByParentAssetIdWithAssociationsOrderByCreatedAtAsc(assetId))
                .thenReturn(java.util.List.of(child));
        when(transferRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        AssetTransferRequest result = service.create(new TransferCreateCommand(assetId, toOrgNodeId, null,
                "Relocation", approverId,
                java.util.Map.of(childId, com.iams.lifecycle.domain.ChildDisposition.MOVE_WITH_PARENT)));

        assertThat(result.getChildDispositions()).containsEntry(childId.toString(), "MOVE_WITH_PARENT");
    }

    @Test
    void approve_appliesMoveWithParentToChild() {
        // US-AST-04: a child dispositioned MOVE_WITH_PARENT relocates to the parent's new org node on approval.
        AssetTransferRequest request = pendingRequest();
        Asset child = new Asset();
        UUID childId = UUID.randomUUID();
        child.setId(childId);
        child.setAssetNumber("AST-CHILD-1");
        child.setOrgNode(fromNode);
        child.setParentAsset(asset);
        request.getChildDispositions().put(childId.toString(), "MOVE_WITH_PARENT");
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(assetRepository.findByIdWithAssociations(childId)).thenReturn(Optional.of(child));
        when(assetRepository.saveAndFlush(child)).thenReturn(child);
        when(historyRecorder.record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(new AssetHistoryEvent());
        when(transferRepository.saveAndFlush(request)).thenReturn(request);

        service.approve(request.getId());

        assertThat(child.getOrgNode()).isEqualTo(toNode);
    }

    @Test
    void approve_appliesDetachToChild() {
        AssetTransferRequest request = pendingRequest();
        Asset child = new Asset();
        UUID childId = UUID.randomUUID();
        child.setId(childId);
        child.setAssetNumber("AST-CHILD-1");
        child.setOrgNode(fromNode);
        child.setParentAsset(asset);
        request.getChildDispositions().put(childId.toString(), "DETACH");
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(assetRepository.findByIdWithAssociations(childId)).thenReturn(Optional.of(child));
        when(assetRepository.saveAndFlush(child)).thenReturn(child);
        when(historyRecorder.record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(new AssetHistoryEvent());
        when(transferRepository.saveAndFlush(request)).thenReturn(request);

        service.approve(request.getId());

        assertThat(child.getParentAsset()).isNull();
        assertThat(child.getOrgNode()).isEqualTo(fromNode); // detached in place, not moved
    }

    @Test
    void approve_movesAssetToNewOrgNode_whenActedByRoutedApprover() {
        AssetTransferRequest request = pendingRequest();
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(historyRecorder.record(org.mockito.ArgumentMatchers.eq(asset), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(new AssetHistoryEvent());
        when(transferRepository.saveAndFlush(request)).thenReturn(request);

        AssetTransferRequest result = service.approve(request.getId());

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.APPROVED);
        assertThat(asset.getOrgNode()).isEqualTo(toNode);
        org.mockito.Mockito.verify(auditScopeChangeService).flagIfInActiveAudit(asset);
    }

    @Test
    void approve_blocksWhenAssetUnderActiveLegalHold() {
        AssetTransferRequest request = pendingRequest();
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);
        org.mockito.Mockito.doThrow(new com.iams.compliance.application.LegalHoldActiveException(com.iams.compliance.domain.LegalHoldScopeType.ASSET))
                .when(legalHoldService).requireNoActiveHold(com.iams.compliance.domain.LegalHoldScopeType.ASSET, assetId);

        assertThatThrownBy(() -> service.approve(request.getId()))
                .isInstanceOf(com.iams.compliance.application.LegalHoldActiveException.class);
        assertThat(asset.getOrgNode()).isEqualTo(fromNode);
    }

    @Test
    void approve_rejectsAnyoneOtherThanRoutedApprover() {
        AssetTransferRequest request = pendingRequest();
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);

        assertThatThrownBy(() -> service.approve(request.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approve_blocksSelfApproval_withoutWaiver() {
        // AC-USR-06-X: the requester routed the transfer to themselves and tries to approve it.
        AssetTransferRequest request = pendingRequest();
        request.setNominalApproverId(actorId);
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(routingService.resolveEffectiveApprover(actorId)).thenReturn(actorId);
        when(sodWaiverRepository.existsByScopeAndActiveTrue("TRANSFER_APPROVAL")).thenReturn(false);

        assertThatThrownBy(() -> service.approve(request.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Separation of duties");
        assertThat(asset.getOrgNode()).isEqualTo(fromNode); // nothing moved
    }

    @Test
    void approve_allowsSelfApproval_withActiveWaiver() {
        // AC-USR-06-X converse: a recorded, active TRANSFER_APPROVAL waiver permits the self-approval.
        AssetTransferRequest request = pendingRequest();
        request.setNominalApproverId(actorId);
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(routingService.resolveEffectiveApprover(actorId)).thenReturn(actorId);
        when(sodWaiverRepository.existsByScopeAndActiveTrue("TRANSFER_APPROVAL")).thenReturn(true);
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(historyRecorder.record(org.mockito.ArgumentMatchers.eq(asset), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(new AssetHistoryEvent());
        when(transferRepository.saveAndFlush(request)).thenReturn(request);

        AssetTransferRequest result = service.approve(request.getId());

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.APPROVED);
        assertThat(asset.getOrgNode()).isEqualTo(toNode);
    }

    @Test
    void approve_rejectsWhenNotPending() {
        AssetTransferRequest request = pendingRequest();
        request.setStatus(LifecycleRequestStatus.APPROVED);
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(request.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void reject_requiresReason() {
        AssetTransferRequest request = pendingRequest();

        assertThatThrownBy(() -> service.reject(request.getId(), " ")).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void reject_setsRejectedStatusAndReason() {
        AssetTransferRequest request = pendingRequest();
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);
        when(transferRepository.saveAndFlush(request)).thenReturn(request);

        AssetTransferRequest result = service.reject(request.getId(), "Wrong destination");

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Wrong destination");
    }

    @Test
    void escalate_blocksBeforeThresholdReached() {
        AssetTransferRequest request = pendingRequest();
        request.setRequestedAt(Instant.now());
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(lifecycleProperties.getEscalationThresholdHours()).thenReturn(72);

        assertThatThrownBy(() -> service.escalate(request.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void escalate_resolvesEscalationTarget_oncePastThreshold() {
        AssetTransferRequest request = pendingRequest();
        request.setRequestedAt(Instant.now().minus(java.time.Duration.ofHours(100)));
        when(transferRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(lifecycleProperties.getEscalationThresholdHours()).thenReturn(72);
        UUID escalationTarget = UUID.randomUUID();
        when(routingService.resolveEscalationTarget(approverId)).thenReturn(escalationTarget);
        when(transferRepository.saveAndFlush(request)).thenReturn(request);

        AssetTransferRequest result = service.escalate(request.getId());

        assertThat(result.getEffectiveApproverId()).isEqualTo(escalationTarget);
    }

    private AssetTransferRequest pendingRequest() {
        AssetTransferRequest request = new AssetTransferRequest();
        request.setId(UUID.randomUUID());
        request.setAsset(asset);
        request.setFromOrgNode(fromNode);
        request.setToOrgNode(toNode);
        request.setReason("Relocation");
        request.setStatus(LifecycleRequestStatus.PENDING);
        request.setNominalApproverId(approverId);
        request.setRequestedBy(actorId);
        request.setRequestedAt(Instant.now());
        return request;
    }
}
