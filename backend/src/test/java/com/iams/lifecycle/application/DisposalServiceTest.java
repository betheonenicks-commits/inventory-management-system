package com.iams.lifecycle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.asset.application.AssetHistoryRecorder;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetHistoryEventRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.audit.application.AuditScopeChangeService;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.application.LegalHoldService;
import com.iams.lifecycle.domain.AssetDisposalRequest;
import com.iams.lifecycle.domain.AssetDisposalRequestRepository;
import com.iams.lifecycle.domain.DisposalType;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.domain.AppUserRepository;
import java.time.Duration;
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
class DisposalServiceTest {

    @Mock private AssetDisposalRequestRepository disposalRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private AssetStatusDefRepository statusDefRepository;
    @Mock private AssetHistoryRecorder historyRecorder;
    @Mock private AssetHistoryEventRepository historyEventRepository;
    @Mock private ApprovalRoutingService routingService;
    @Mock private AuditScopeChangeService auditScopeChangeService;
    @Mock private AppUserRepository appUserRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OrgScopeGuard scopeGuard;
    @Mock private LifecycleProperties lifecycleProperties;
    @Mock private LegalHoldService legalHoldService;

    private DisposalService service;
    private UUID actorId;
    private UUID approverId;
    private Asset asset;

    @BeforeEach
    void setUp() {
        service = new DisposalService(disposalRepository, assetRepository, statusDefRepository, historyRecorder,
                historyEventRepository, routingService, auditScopeChangeService, appUserRepository, currentUserProvider,
                scopeGuard, lifecycleProperties, legalHoldService);
        actorId = UUID.randomUUID();
        approverId = UUID.randomUUID();
        asset = new Asset();
        asset.setId(UUID.randomUUID());
        var orgNode = new com.iams.org.domain.OrgNode();
        orgNode.setId(UUID.randomUUID());
        asset.setOrgNode(orgNode);

        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(actorId, "manager1", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void create_rejectsBlankReason() {
        assertThatThrownBy(() -> service.create(new DisposalCreateCommand(asset.getId(), DisposalType.RETIRE, " ", approverId)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_succeeds_withValidReasonAndApprover() {
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(appUserRepository.existsById(approverId)).thenReturn(true);
        when(disposalRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        AssetDisposalRequest result = service.create(new DisposalCreateCommand(asset.getId(), DisposalType.DONATE, "End of life", approverId));

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.PENDING);
        assertThat(result.getDisposalType()).isEqualTo(DisposalType.DONATE);
    }

    @Test
    void approve_setsRetiredStatus_forRetireType() {
        AssetDisposalRequest request = pendingRequest(DisposalType.RETIRE);
        when(disposalRepository.findByIdWithAsset(request.getId())).thenReturn(Optional.of(request));
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);
        AssetStatusDef retired = new AssetStatusDef();
        retired.setCode("RETIRED");
        when(statusDefRepository.findByCode("RETIRED")).thenReturn(Optional.of(retired));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(historyRecorder.record(org.mockito.ArgumentMatchers.eq(asset), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AssetHistoryEvent());
        when(disposalRepository.saveAndFlush(request)).thenReturn(request);

        AssetDisposalRequest result = service.approve(request.getId());

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.APPROVED);
        assertThat(asset.getStatus()).isEqualTo(retired);
        org.mockito.Mockito.verify(auditScopeChangeService).flagIfInActiveAudit(asset);
    }

    @Test
    void approve_rejectsAnyoneOtherThanRoutedApprover() {
        AssetDisposalRequest request = pendingRequest(DisposalType.DISPOSE);
        when(disposalRepository.findByIdWithAsset(request.getId())).thenReturn(Optional.of(request));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);

        assertThatThrownBy(() -> service.approve(request.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void restore_revertsToInStorage_withinWindow() {
        AssetDisposalRequest request = pendingRequest(DisposalType.DISPOSE);
        request.setStatus(LifecycleRequestStatus.APPROVED);
        request.setDecidedAt(Instant.now().minus(Duration.ofDays(10)));
        when(disposalRepository.findByIdWithAsset(request.getId())).thenReturn(Optional.of(request));
        when(lifecycleProperties.getRestoreWindowDays()).thenReturn(30);
        AssetStatusDef inStorage = new AssetStatusDef();
        inStorage.setCode("IN_STORAGE");
        when(statusDefRepository.findByCode("IN_STORAGE")).thenReturn(Optional.of(inStorage));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(disposalRepository.saveAndFlush(request)).thenReturn(request);

        AssetDisposalRequest result = service.restore(request.getId());

        assertThat(result.getRestoredAt()).isNotNull();
        assertThat(asset.getStatus()).isEqualTo(inStorage);
    }

    @Test
    void restore_blocksOnceWindowElapsed() {
        AssetDisposalRequest request = pendingRequest(DisposalType.DISPOSE);
        request.setStatus(LifecycleRequestStatus.APPROVED);
        request.setDecidedAt(Instant.now().minus(Duration.ofDays(40)));
        when(disposalRepository.findByIdWithAsset(request.getId())).thenReturn(Optional.of(request));
        when(lifecycleProperties.getRestoreWindowDays()).thenReturn(30);

        assertThatThrownBy(() -> service.restore(request.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void restore_blocksWhenAlreadyRestored() {
        AssetDisposalRequest request = pendingRequest(DisposalType.DISPOSE);
        request.setStatus(LifecycleRequestStatus.APPROVED);
        request.setRestoredAt(Instant.now());
        when(disposalRepository.findByIdWithAsset(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.restore(request.getId())).isInstanceOf(ConflictException.class);
    }

    private AssetDisposalRequest pendingRequest(DisposalType type) {
        AssetDisposalRequest request = new AssetDisposalRequest();
        request.setId(UUID.randomUUID());
        request.setAsset(asset);
        request.setDisposalType(type);
        request.setReason("End of life");
        request.setStatus(LifecycleRequestStatus.PENDING);
        request.setNominalApproverId(approverId);
        request.setRequestedBy(actorId);
        request.setRequestedAt(Instant.now());
        return request;
    }
}
