package com.iams.lifecycle.application;

import com.iams.asset.application.AssetHistoryRecorder;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetHistoryEventRepository;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.audit.application.AuditScopeChangeService;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.application.LegalHoldService;
import com.iams.compliance.domain.LegalHoldScopeType;
import com.iams.lifecycle.domain.AssetDisposalRequest;
import com.iams.lifecycle.domain.AssetDisposalRequestRepository;
import com.iams.lifecycle.domain.DisposalType;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
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
 * US-LIF-09/10/11/12/13: request → approve/reject a retirement/disposal/
 * donation with a mandatory reason, restorable within a configurable window.
 * <p>
 * US-USR-06 (AC-USR-06-X): the requester cannot approve their own disposal -
 * blocked at approve() unless an active DISPOSAL_APPROVAL SoD waiver exists,
 * reusing the same generic sod_waiver mechanism EPIC-AUD uses for AUDIT_APPROVAL.
 */
@Service
public class DisposalService {

    private static final String RETIRED_STATUS_CODE = "RETIRED";
    private static final String DISPOSED_STATUS_CODE = "DISPOSED";
    /** The sod_waiver scope string a documented exception to disposal self-approval is recorded under. */
    private static final String SOD_SCOPE = "DISPOSAL_APPROVAL";

    private final AssetDisposalRequestRepository disposalRepository;
    private final AssetRepository assetRepository;
    private final AssetStatusDefRepository statusDefRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final AssetHistoryEventRepository historyEventRepository;
    private final ApprovalRoutingService routingService;
    private final AuditScopeChangeService auditScopeChangeService;
    private final AppUserRepository appUserRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrgScopeGuard scopeGuard;
    private final LifecycleProperties lifecycleProperties;
    private final LegalHoldService legalHoldService;
    private final SodWaiverRepository sodWaiverRepository;

    public DisposalService(AssetDisposalRequestRepository disposalRepository, AssetRepository assetRepository,
                            AssetStatusDefRepository statusDefRepository, AssetHistoryRecorder historyRecorder,
                            AssetHistoryEventRepository historyEventRepository, ApprovalRoutingService routingService,
                            AuditScopeChangeService auditScopeChangeService, AppUserRepository appUserRepository,
                            CurrentUserProvider currentUserProvider, OrgScopeGuard scopeGuard,
                            LifecycleProperties lifecycleProperties, LegalHoldService legalHoldService,
                            SodWaiverRepository sodWaiverRepository) {
        this.disposalRepository = disposalRepository;
        this.assetRepository = assetRepository;
        this.statusDefRepository = statusDefRepository;
        this.historyRecorder = historyRecorder;
        this.historyEventRepository = historyEventRepository;
        this.routingService = routingService;
        this.auditScopeChangeService = auditScopeChangeService;
        this.appUserRepository = appUserRepository;
        this.currentUserProvider = currentUserProvider;
        this.scopeGuard = scopeGuard;
        this.lifecycleProperties = lifecycleProperties;
        this.legalHoldService = legalHoldService;
        this.sodWaiverRepository = sodWaiverRepository;
    }

    @Transactional
    public AssetDisposalRequest create(DisposalCreateCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            // AC-LIF-09-X: rejected before it reaches an approver.
            throw ValidationFailedException.singleField("reason", "A reason is required to request a disposal");
        }
        Asset asset = assetRepository.findByIdWithAssociations(command.assetId())
                .orElseThrow(() -> NotFoundException.of("Asset", command.assetId()));
        scopeGuard.requireWithinScope(asset.getOrgNode().getId(), "asset", asset.getId());
        if (!appUserRepository.existsById(command.nominalApproverId())) {
            throw NotFoundException.of("AppUser", command.nominalApproverId());
        }

        UUID actor = currentUserProvider.current().id();
        AssetDisposalRequest request = new AssetDisposalRequest();
        request.setAsset(asset);
        request.setDisposalType(command.disposalType());
        request.setReason(command.reason());
        request.setStatus(LifecycleRequestStatus.PENDING);
        request.setNominalApproverId(command.nominalApproverId());
        request.setRequestedBy(actor);
        request.setRequestedAt(Instant.now());
        request.setCreatedBy(actor);
        return disposalRepository.save(request);
    }

    @Transactional(readOnly = true)
    public AssetDisposalRequest get(UUID id) {
        return disposalRepository.findByIdWithAsset(id).orElseThrow(() -> NotFoundException.of("AssetDisposalRequest", id));
    }

    @Transactional(readOnly = true)
    public List<AssetDisposalRequest> list(UUID assetId, LifecycleRequestStatus status) {
        if (assetId != null) {
            return disposalRepository.findByAssetIdWithAssetOrderByRequestedAtDesc(assetId);
        }
        if (status != null) {
            return disposalRepository.findByStatusWithAssetOrderByRequestedAtDesc(status);
        }
        return disposalRepository.findAllWithAssetOrderByRequestedAtDesc();
    }

    @Transactional
    public AssetDisposalRequest approve(UUID id) {
        AssetDisposalRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        UUID actor = currentUserProvider.current().id();
        requireIsRoutedApprover(request, actor);
        // AC-USR-06-X: the requester can't approve their own disposal without a recorded waiver.
        requireNotSelfApproval(request.getRequestedBy(), actor);
        // AC-CMP-06-H: a legal hold on this asset blocks disposal until it's lifted.
        legalHoldService.requireNoActiveHold(LegalHoldScopeType.ASSET, request.getAsset().getId());

        Asset asset = request.getAsset();
        String targetStatusCode = request.getDisposalType() == DisposalType.RETIRE ? RETIRED_STATUS_CODE : DISPOSED_STATUS_CODE;
        AssetStatusDef targetStatus = statusDefRepository.findByCode(targetStatusCode)
                .orElseThrow(() -> new IllegalStateException(targetStatusCode + " status missing from seed data"));
        String previousStatusCode = asset.getStatus() != null ? asset.getStatus().getCode() : null;

        asset.setStatus(targetStatus);
        asset.setUpdatedBy(actor);
        asset = assetRepository.saveAndFlush(asset);
        AssetHistoryEvent event = historyRecorder.record(asset, AssetHistoryEventType.LIFECYCLE_EVENT, "status",
                previousStatusCode, targetStatusCode + " (" + request.getDisposalType() + ": " + request.getReason() + ")");

        request.setStatus(LifecycleRequestStatus.APPROVED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request.setDisposalHistoryEvent(event);
        request.setUpdatedBy(actor);
        request = disposalRepository.saveAndFlush(request);

        // US-AUD-23: same automatic trigger transfers use - a disposed asset that's part of an in-progress, not-yet-scanned audit.
        auditScopeChangeService.flagIfInActiveAudit(asset);
        return request;
    }

    @Transactional
    public AssetDisposalRequest reject(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ValidationFailedException.singleField("reason", "A reason is required to reject a disposal request");
        }
        AssetDisposalRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        UUID actor = currentUserProvider.current().id();
        requireIsRoutedApprover(request, actor);

        request.setStatus(LifecycleRequestStatus.REJECTED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request.setRejectionReason(reason);
        request.setUpdatedBy(actor);
        return disposalRepository.saveAndFlush(request);
    }

    /** US-LIF-13 (Partial - see ApprovalRoutingService's Javadoc). */
    @Transactional
    public AssetDisposalRequest escalate(UUID id) {
        AssetDisposalRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        Duration age = Duration.between(request.getRequestedAt(), Instant.now());
        if (age.toHours() < lifecycleProperties.getEscalationThresholdHours()) {
            throw new ConflictException("ESCALATION_THRESHOLD_NOT_REACHED",
                    "This request has not been pending long enough to escalate (threshold: "
                            + lifecycleProperties.getEscalationThresholdHours() + "h)");
        }
        UUID currentApprover = request.getEffectiveApproverId() != null ? request.getEffectiveApproverId() : request.getNominalApproverId();
        request.setEffectiveApproverId(routingService.resolveEscalationTarget(currentApprover));
        request.setUpdatedBy(currentUserProvider.current().id());
        return disposalRepository.saveAndFlush(request);
    }

    /**
     * US-LIF-12: restore within a configurable window - AC-LIF-12-H requires
     * the original disposal event survive untouched, with a new "restored"
     * event linked to it via correctionOfEvent, not an edit.
     */
    @Transactional
    public AssetDisposalRequest restore(UUID id) {
        AssetDisposalRequest request = get(id);
        if (request.getStatus() != LifecycleRequestStatus.APPROVED) {
            throw new ConflictException("DISPOSAL_NOT_APPROVED", "Only an approved disposal can be restored");
        }
        if (request.getRestoredAt() != null) {
            throw new ConflictException("ALREADY_RESTORED", "This disposal has already been restored");
        }
        Duration sinceDisposal = Duration.between(request.getDecidedAt(), Instant.now());
        if (sinceDisposal.toDays() > lifecycleProperties.getRestoreWindowDays()) {
            // AC-LIF-12-X: blocked once the restore window has elapsed.
            throw new ConflictException("RESTORE_WINDOW_ELAPSED",
                    "The " + lifecycleProperties.getRestoreWindowDays() + "-day restore window for this disposal has elapsed");
        }

        Asset asset = request.getAsset();
        AssetStatusDef inStorage = statusDefRepository.findByCode("IN_STORAGE")
                .orElseThrow(() -> new IllegalStateException("IN_STORAGE status missing from seed data"));
        String previousStatusCode = asset.getStatus() != null ? asset.getStatus().getCode() : null;

        UUID actor = currentUserProvider.current().id();
        asset.setStatus(inStorage);
        asset.setUpdatedBy(actor);
        asset = assetRepository.saveAndFlush(asset);

        AssetHistoryEvent disposalEvent = request.getDisposalHistoryEvent() != null
                ? historyEventRepository.findById(request.getDisposalHistoryEvent().getId()).orElse(null)
                : null;
        historyRecorder.record(asset, AssetHistoryEventType.CORRECTION, "status", previousStatusCode,
                inStorage.getCode() + " (restored from disposal)", disposalEvent);

        request.setRestoredAt(Instant.now());
        request.setRestoredBy(actor);
        request.setUpdatedBy(actor);
        return disposalRepository.saveAndFlush(request);
    }

    private void requireIsRoutedApprover(AssetDisposalRequest request, UUID actor) {
        UUID approver = request.getEffectiveApproverId() != null
                ? request.getEffectiveApproverId()
                : routingService.resolveEffectiveApprover(request.getNominalApproverId());
        if (!approver.equals(actor)) {
            throw new AccessDeniedException("Only this disposal request's routed approver may act on it");
        }
    }

    /**
     * US-USR-06 (AC-USR-06-X): separation of duties - a requester can't approve their own
     * disposal. An active DISPOSAL_APPROVAL SoD waiver (a signed-off, documented exception)
     * is the one way through, mirroring EPIC-AUD's AUDIT_APPROVAL handling.
     */
    private void requireNotSelfApproval(UUID requestedBy, UUID actor) {
        if (actor.equals(requestedBy) && !sodWaiverRepository.existsByScopeAndActiveTrue(SOD_SCOPE)) {
            throw new AccessDeniedException("Separation of duties: you cannot approve a disposal you requested "
                    + "yourself. Route it to another approver, or record an active '" + SOD_SCOPE + "' SoD waiver first.");
        }
    }

    private AssetDisposalRequest requireStatus(UUID id, LifecycleRequestStatus expected) {
        AssetDisposalRequest request = get(id);
        if (request.getStatus() != expected) {
            throw new ConflictException("DISPOSAL_WRONG_STATUS",
                    "Disposal request must be " + expected + "; current status is " + request.getStatus());
        }
        return request;
    }
}
