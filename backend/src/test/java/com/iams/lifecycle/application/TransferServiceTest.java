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
                scopeGuard, lifecycleProperties, legalHoldService);
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

        AssetTransferRequest result = service.create(new TransferCreateCommand(assetId, toOrgNodeId, null, "Relocation", approverId));

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.PENDING);
        assertThat(result.getFromOrgNode()).isEqualTo(fromNode);
        assertThat(result.getToOrgNode()).isEqualTo(toNode);
        assertThat(result.getRequestedBy()).isEqualTo(actorId);
    }

    @Test
    void create_rejectsBlankReason() {
        assertThatThrownBy(() -> service.create(new TransferCreateCommand(assetId, toOrgNodeId, null, " ", approverId)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsUnknownApprover() {
        when(assetRepository.findByIdWithAssociations(assetId)).thenReturn(Optional.of(asset));
        when(orgNodeRepository.findById(toOrgNodeId)).thenReturn(Optional.of(toNode));
        when(appUserRepository.existsById(approverId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new TransferCreateCommand(assetId, toOrgNodeId, null, "Relocation", approverId)))
                .isInstanceOf(NotFoundException.class);
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
