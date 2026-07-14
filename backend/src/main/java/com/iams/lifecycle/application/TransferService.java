package com.iams.lifecycle.application;

import com.iams.asset.application.AssetAssignmentService;
import com.iams.asset.application.AssetHistoryRecorder;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.audit.application.AuditScopeChangeService;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.lifecycle.domain.AssetTransferRequest;
import com.iams.lifecycle.domain.AssetTransferRequestRepository;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.domain.AppUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-LIF-05/10/11/13: request → approve/reject an asset transfer between org
 * nodes and/or custodians. Approval always required - "per policy" (the
 * story's own wording) implies a configurable policy engine that doesn't
 * exist anywhere in this codebase, so every transfer requires approval
 * unconditionally, the simplest safe reading. No SoD self-approval check
 * (unlike EPIC-AUD) - no story here names that conflict, and inventing one
 * would be scope no story asked for.
 */
@Service
public class TransferService {

    private final AssetTransferRequestRepository transferRepository;
    private final AssetRepository assetRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final AssetAssignmentService assignmentService;
    private final ApprovalRoutingService routingService;
    private final AuditScopeChangeService auditScopeChangeService;
    private final AppUserRepository appUserRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrgScopeGuard scopeGuard;
    private final LifecycleProperties lifecycleProperties;

    public TransferService(AssetTransferRequestRepository transferRepository, AssetRepository assetRepository,
                            OrgNodeRepository orgNodeRepository, AssetHistoryRecorder historyRecorder,
                            AssetAssignmentService assignmentService, ApprovalRoutingService routingService,
                            AuditScopeChangeService auditScopeChangeService, AppUserRepository appUserRepository,
                            CurrentUserProvider currentUserProvider, OrgScopeGuard scopeGuard,
                            LifecycleProperties lifecycleProperties) {
        this.transferRepository = transferRepository;
        this.assetRepository = assetRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.historyRecorder = historyRecorder;
        this.assignmentService = assignmentService;
        this.routingService = routingService;
        this.auditScopeChangeService = auditScopeChangeService;
        this.appUserRepository = appUserRepository;
        this.currentUserProvider = currentUserProvider;
        this.scopeGuard = scopeGuard;
        this.lifecycleProperties = lifecycleProperties;
    }

    @Transactional
    public AssetTransferRequest create(TransferCreateCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            throw ValidationFailedException.singleField("reason", "A reason is required to request a transfer");
        }
        Asset asset = assetRepository.findByIdWithAssociations(command.assetId())
                .orElseThrow(() -> NotFoundException.of("Asset", command.assetId()));
        scopeGuard.requireWithinScope(asset.getOrgNode().getId(), "asset", asset.getId());

        OrgNode toOrgNode = orgNodeRepository.findById(command.toOrgNodeId())
                .orElseThrow(() -> NotFoundException.of("OrgNode", command.toOrgNodeId()));
        if (!appUserRepository.existsById(command.nominalApproverId())) {
            throw NotFoundException.of("AppUser", command.nominalApproverId());
        }

        UUID actor = currentUserProvider.current().id();
        AssetTransferRequest request = new AssetTransferRequest();
        request.setAsset(asset);
        request.setFromOrgNode(asset.getOrgNode());
        request.setToOrgNode(toOrgNode);
        request.setFromPersonId(asset.getAssignedToPersonId());
        request.setToPersonId(command.toPersonId());
        request.setReason(command.reason());
        request.setStatus(LifecycleRequestStatus.PENDING);
        request.setNominalApproverId(command.nominalApproverId());
        request.setRequestedBy(actor);
        request.setRequestedAt(Instant.now());
        request.setCreatedBy(actor);
        return transferRepository.save(request);
    }

    @Transactional(readOnly = true)
    public AssetTransferRequest get(UUID id) {
        return transferRepository.findByIdWithAssociations(id).orElseThrow(() -> NotFoundException.of("AssetTransferRequest", id));
    }

    @Transactional(readOnly = true)
    public List<AssetTransferRequest> list(UUID assetId, LifecycleRequestStatus status) {
        if (assetId != null) {
            return transferRepository.findByAssetIdWithAssociationsOrderByRequestedAtDesc(assetId);
        }
        if (status != null) {
            return transferRepository.findByStatusWithAssociationsOrderByRequestedAtDesc(status);
        }
        return transferRepository.findAllWithAssociationsOrderByRequestedAtDesc();
    }

    @Transactional
    public AssetTransferRequest approve(UUID id) {
        AssetTransferRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        UUID actor = currentUserProvider.current().id();
        requireIsRoutedApprover(request, actor);

        Asset asset = request.getAsset();
        String fromCode = request.getFromOrgNode() != null ? request.getFromOrgNode().getCode() : null;
        asset.setOrgNode(request.getToOrgNode());
        asset.setUpdatedBy(actor);
        asset = assetRepository.saveAndFlush(asset);
        historyRecorder.record(asset, AssetHistoryEventType.LOCATION_CHANGE, "orgNode", fromCode, request.getToOrgNode().getCode());

        if (request.getToPersonId() != null && !request.getToPersonId().equals(request.getFromPersonId())) {
            asset = assignmentService.assign(asset.getId(), request.getToPersonId(), asset.getVersion());
        }

        request.setStatus(LifecycleRequestStatus.APPROVED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request.setUpdatedBy(actor);
        request = transferRepository.saveAndFlush(request);

        // US-AUD-23: flag this asset in any in-progress audit that expects it and hasn't scanned it yet.
        auditScopeChangeService.flagIfInActiveAudit(asset);
        return request;
    }

    @Transactional
    public AssetTransferRequest reject(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            // AC-LIF-11-X: blocked until a reason is supplied.
            throw ValidationFailedException.singleField("reason", "A reason is required to reject a transfer");
        }
        AssetTransferRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        UUID actor = currentUserProvider.current().id();
        requireIsRoutedApprover(request, actor);

        request.setStatus(LifecycleRequestStatus.REJECTED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request.setRejectionReason(reason);
        request.setUpdatedBy(actor);
        return transferRepository.saveAndFlush(request);
    }

    /** US-LIF-13 (Partial - see ApprovalRoutingService's Javadoc for what's not automatic here). */
    @Transactional
    public AssetTransferRequest escalate(UUID id) {
        AssetTransferRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        Duration age = Duration.between(request.getRequestedAt(), Instant.now());
        if (age.toHours() < lifecycleProperties.getEscalationThresholdHours()) {
            throw new ConflictException("ESCALATION_THRESHOLD_NOT_REACHED",
                    "This request has not been pending long enough to escalate (threshold: "
                            + lifecycleProperties.getEscalationThresholdHours() + "h)");
        }
        UUID currentApprover = request.getEffectiveApproverId() != null ? request.getEffectiveApproverId() : request.getNominalApproverId();
        request.setEffectiveApproverId(routingService.resolveEscalationTarget(currentApprover));
        request.setUpdatedBy(currentUserProvider.current().id());
        return transferRepository.saveAndFlush(request);
    }

    private void requireIsRoutedApprover(AssetTransferRequest request, UUID actor) {
        UUID approver = request.getEffectiveApproverId() != null
                ? request.getEffectiveApproverId()
                : routingService.resolveEffectiveApprover(request.getNominalApproverId());
        if (!approver.equals(actor)) {
            throw new AccessDeniedException("Only this transfer's routed approver may act on it");
        }
    }

    private AssetTransferRequest requireStatus(UUID id, LifecycleRequestStatus expected) {
        AssetTransferRequest request = get(id);
        if (request.getStatus() != expected) {
            throw new ConflictException("TRANSFER_WRONG_STATUS",
                    "Transfer request must be " + expected + "; current status is " + request.getStatus());
        }
        return request;
    }
}
