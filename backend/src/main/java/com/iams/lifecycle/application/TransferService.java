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
import org.springframework.context.ApplicationEventPublisher;
import com.iams.compliance.application.LegalHoldService;
import com.iams.compliance.domain.LegalHoldScopeType;
import com.iams.lifecycle.domain.AssetTransferRequest;
import com.iams.lifecycle.domain.AssetTransferRequestRepository;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.SodWaiverRepository;
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
 * unconditionally, the simplest safe reading.
 * <p>
 * US-USR-06 (AC-USR-06-X): the requester cannot approve their own transfer -
 * blocked at approve() unless an active TRANSFER_APPROVAL SoD waiver exists,
 * reusing the same generic sod_waiver mechanism EPIC-AUD already uses for
 * AUDIT_APPROVAL.
 */
@Service
public class TransferService {

    /** The sod_waiver scope string a documented exception to transfer self-approval is recorded under (mirrors AUDIT_APPROVAL). */
    private static final String SOD_SCOPE = "TRANSFER_APPROVAL";

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
    private final LegalHoldService legalHoldService;
    private final ApplicationEventPublisher eventPublisher;
    private final SodWaiverRepository sodWaiverRepository;

    public TransferService(AssetTransferRequestRepository transferRepository, AssetRepository assetRepository,
                            OrgNodeRepository orgNodeRepository, AssetHistoryRecorder historyRecorder,
                            AssetAssignmentService assignmentService, ApprovalRoutingService routingService,
                            AuditScopeChangeService auditScopeChangeService, AppUserRepository appUserRepository,
                            CurrentUserProvider currentUserProvider, OrgScopeGuard scopeGuard,
                            LifecycleProperties lifecycleProperties, LegalHoldService legalHoldService,
                            ApplicationEventPublisher eventPublisher, SodWaiverRepository sodWaiverRepository) {
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
        this.legalHoldService = legalHoldService;
        this.eventPublisher = eventPublisher;
        this.sodWaiverRepository = sodWaiverRepository;
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
        // AC-USR-06-X: the requester can't approve their own transfer without a recorded waiver.
        requireNotSelfApproval(request.getRequestedBy(), actor);
        // AC-CMP-06-H: a legal hold on this asset blocks transfer, same as disposal - found
        // as a real, exploitable gap by an adversarial review (a held asset could otherwise
        // be moved away, defeating the hold's purpose for anything except disposal).
        legalHoldService.requireNoActiveHold(LegalHoldScopeType.ASSET, request.getAsset().getId());

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
        // US-NTF-04: same transaction, so the notification commits with the decision.
        eventPublisher.publishEvent(new TransferDecidedEvent(request.getId(), asset.getId(), asset.getName(),
                "approved", null, request.getCreatedBy(), request.getFromPersonId(), request.getToPersonId(),
                currentUserProvider.current().username()));
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
        request = transferRepository.saveAndFlush(request);
        // US-NTF-04's rejection AC: requester and affected holder are notified WITH the reason.
        eventPublisher.publishEvent(new TransferDecidedEvent(request.getId(), request.getAsset().getId(),
                request.getAsset().getName(), "rejected", reason, request.getCreatedBy(),
                request.getFromPersonId(), request.getToPersonId(), currentUserProvider.current().username()));
        return request;
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

    /**
     * US-USR-06 (AC-USR-06-X): separation of duties - a requester approving their own
     * submission is the exact conflict this guards. An active TRANSFER_APPROVAL SoD waiver
     * (an IT Security Officer's signed-off, documented exception) is the one way through,
     * consistent with how EPIC-AUD treats AUDIT_APPROVAL - an exception is always recorded,
     * never silent.
     */
    private void requireNotSelfApproval(UUID requestedBy, UUID actor) {
        if (actor.equals(requestedBy) && !sodWaiverRepository.existsByScopeAndActiveTrue(SOD_SCOPE)) {
            throw new AccessDeniedException("Separation of duties: you cannot approve a transfer you requested "
                    + "yourself. Route it to another approver, or record an active '" + SOD_SCOPE + "' SoD waiver first.");
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
