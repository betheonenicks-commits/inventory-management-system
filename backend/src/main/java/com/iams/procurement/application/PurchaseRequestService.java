package com.iams.procurement.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.lifecycle.application.ApprovalRoutingService;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.procurement.domain.PurchaseRequest;
import com.iams.procurement.domain.PurchaseRequestRepository;
import com.iams.usr.domain.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-LIF-01: submit a purchase request that routes to an approver.
 * Reuses {@link ApprovalRoutingService} for delegation/escalation - the
 * exact same approval-routing shape Transfer/Disposal requests already use,
 * just for a different resource type.
 */
@Service
public class PurchaseRequestService {

    private final PurchaseRequestRepository requestRepository;
    private final AppUserRepository appUserRepository;
    private final ApprovalRoutingService routingService;
    private final CurrentUserProvider currentUserProvider;

    public PurchaseRequestService(PurchaseRequestRepository requestRepository, AppUserRepository appUserRepository,
                                   ApprovalRoutingService routingService, CurrentUserProvider currentUserProvider) {
        this.requestRepository = requestRepository;
        this.appUserRepository = appUserRepository;
        this.routingService = routingService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public PurchaseRequest create(PurchaseRequestCreateCommand command) {
        if (command.justification() == null || command.justification().isBlank()) {
            // AC-LIF-01-X: rejected before it reaches an approver.
            throw ValidationFailedException.singleField("justification", "A justification is required to submit a purchase request");
        }
        if (command.itemDescription() == null || command.itemDescription().isBlank()) {
            throw ValidationFailedException.singleField("itemDescription", "This field is required");
        }
        if (!appUserRepository.existsById(command.nominalApproverId())) {
            throw NotFoundException.of("AppUser", command.nominalApproverId());
        }

        UUID actor = currentUserProvider.current().id();
        PurchaseRequest request = new PurchaseRequest();
        request.setItemDescription(command.itemDescription());
        request.setJustification(command.justification());
        request.setEstimatedCost(command.estimatedCost());
        request.setVendorName(command.vendorName());
        request.setStatus(LifecycleRequestStatus.PENDING);
        request.setNominalApproverId(command.nominalApproverId());
        request.setRequestedBy(actor);
        request.setRequestedAt(Instant.now());
        request.setCreatedBy(actor);
        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public PurchaseRequest get(UUID id) {
        return requestRepository.findById(id).orElseThrow(() -> NotFoundException.of("PurchaseRequest", id));
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequest> list(LifecycleRequestStatus status) {
        if (status != null) {
            return requestRepository.findByStatusOrderByRequestedAtDesc(status);
        }
        return requestRepository.findAllByOrderByRequestedAtDesc();
    }

    @Transactional
    public PurchaseRequest approve(UUID id) {
        PurchaseRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        requireIsRoutedApprover(request, currentUserProvider.current().id());

        UUID actor = currentUserProvider.current().id();
        request.setStatus(LifecycleRequestStatus.APPROVED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request.setUpdatedBy(actor);
        return requestRepository.saveAndFlush(request);
    }

    @Transactional
    public PurchaseRequest reject(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ValidationFailedException.singleField("reason", "A reason is required to reject a purchase request");
        }
        PurchaseRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        requireIsRoutedApprover(request, currentUserProvider.current().id());

        UUID actor = currentUserProvider.current().id();
        request.setStatus(LifecycleRequestStatus.REJECTED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request.setRejectionReason(reason);
        request.setUpdatedBy(actor);
        return requestRepository.saveAndFlush(request);
    }

    private void requireIsRoutedApprover(PurchaseRequest request, UUID actor) {
        UUID approver = request.getEffectiveApproverId() != null
                ? request.getEffectiveApproverId()
                : routingService.resolveEffectiveApprover(request.getNominalApproverId());
        if (!approver.equals(actor)) {
            throw new AccessDeniedException("Only this purchase request's routed approver may act on it");
        }
    }

    private PurchaseRequest requireStatus(UUID id, LifecycleRequestStatus expected) {
        PurchaseRequest request = get(id);
        if (request.getStatus() != expected) {
            throw new ConflictException("PURCHASE_REQUEST_WRONG_STATUS",
                    "Purchase request must be " + expected + "; current status is " + request.getStatus());
        }
        return request;
    }
}
