package com.iams.inventory.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.InventoryItem;
import com.iams.inventory.domain.InventoryItemRepository;
import com.iams.inventory.domain.InventoryManualAdjustmentRequest;
import com.iams.inventory.domain.InventoryManualAdjustmentRequestRepository;
import com.iams.inventory.domain.InventoryStockBalance;
import com.iams.inventory.domain.InventoryTransaction;
import com.iams.inventory.domain.InventoryTransactionRepository;
import com.iams.inventory.domain.InventoryTransactionType;
import com.iams.inventory.domain.Warehouse;
import com.iams.inventory.domain.WarehouseRepository;
import com.iams.lifecycle.application.ApprovalRoutingService;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.usr.domain.AppUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-INV-05: a manual quantity correction changes the balance only after
 * approval - mirrors {@code PurchaseRequestService}'s request/approve/reject
 * shape exactly, including reusing {@link ApprovalRoutingService} for
 * effective-approver delegation.
 */
@Service
public class ManualAdjustmentService {

    private final InventoryManualAdjustmentRequestRepository requestRepository;
    private final InventoryItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final com.iams.inventory.domain.InventoryStockBalanceRepository balanceRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final AppUserRepository appUserRepository;
    private final ApprovalRoutingService routingService;
    private final CurrentUserProvider currentUserProvider;

    public ManualAdjustmentService(InventoryManualAdjustmentRequestRepository requestRepository,
                                    InventoryItemRepository itemRepository, WarehouseRepository warehouseRepository,
                                    com.iams.inventory.domain.InventoryStockBalanceRepository balanceRepository,
                                    InventoryTransactionRepository transactionRepository, AppUserRepository appUserRepository,
                                    ApprovalRoutingService routingService, CurrentUserProvider currentUserProvider) {
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.appUserRepository = appUserRepository;
        this.routingService = routingService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public InventoryManualAdjustmentRequest request(UUID itemId, UUID warehouseId, String subLocation, String lotNumber,
                                                      BigDecimal quantityDelta, String reason, UUID nominalApproverId) {
        // AC-INV-05-X: rejected before it reaches an approver.
        if (reason == null || reason.isBlank()) {
            throw ValidationFailedException.singleField("reason", "A reason is required for a manual adjustment");
        }
        if (quantityDelta == null || quantityDelta.signum() == 0) {
            throw ValidationFailedException.singleField("quantityDelta", "Must be a non-zero quantity change");
        }
        InventoryItem item = itemRepository.findById(itemId).orElseThrow(() -> NotFoundException.of("InventoryItem", itemId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElseThrow(() -> NotFoundException.of("Warehouse", warehouseId));
        if (!appUserRepository.existsById(nominalApproverId)) {
            throw NotFoundException.of("AppUser", nominalApproverId);
        }

        UUID actor = currentUserProvider.current().id();
        InventoryManualAdjustmentRequest request = new InventoryManualAdjustmentRequest();
        request.setInventoryItem(item);
        request.setWarehouse(warehouse);
        request.setSubLocation(subLocation != null ? subLocation.trim() : "");
        request.setLotNumber(lotNumber != null ? lotNumber.trim() : "");
        request.setQuantityDelta(quantityDelta);
        request.setReason(reason);
        request.setStatus(LifecycleRequestStatus.PENDING);
        request.setNominalApproverId(nominalApproverId);
        request.setRequestedBy(actor);
        request.setRequestedAt(Instant.now());
        request.setCreatedBy(actor);
        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public InventoryManualAdjustmentRequest get(UUID id) {
        return requestRepository.findByIdWithAssociations(id).orElseThrow(() -> NotFoundException.of("InventoryManualAdjustmentRequest", id));
    }

    @Transactional(readOnly = true)
    public List<InventoryManualAdjustmentRequest> list(LifecycleRequestStatus status) {
        if (status != null) {
            return requestRepository.findByStatusWithAssociationsOrderByRequestedAtDesc(status);
        }
        return requestRepository.findAllWithAssociationsOrderByRequestedAtDesc();
    }

    /** AC-INV-05-H: quantity changes only after approval - the balance is untouched until this runs. */
    @Transactional
    public InventoryManualAdjustmentRequest approve(UUID id) {
        InventoryManualAdjustmentRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        UUID actor = currentUserProvider.current().id();
        requireIsRoutedApprover(request, actor);

        InventoryStockBalance balance = balanceRepository
                .findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(
                        request.getInventoryItem().getId(), request.getWarehouse().getId(), request.getSubLocation(), request.getLotNumber())
                .orElseGet(() -> {
                    InventoryStockBalance fresh = new InventoryStockBalance();
                    fresh.setInventoryItem(request.getInventoryItem());
                    fresh.setWarehouse(request.getWarehouse());
                    fresh.setSubLocation(request.getSubLocation());
                    fresh.setLotNumber(request.getLotNumber());
                    fresh.setQuantityOnHand(BigDecimal.ZERO);
                    return fresh;
                });
        BigDecimal newQuantity = balance.getQuantityOnHand().add(request.getQuantityDelta());
        if (newQuantity.signum() < 0) {
            throw new ConflictException("ADJUSTMENT_WOULD_GO_NEGATIVE",
                    "This adjustment would take the balance negative (" + balance.getQuantityOnHand() + " on hand, "
                            + request.getQuantityDelta() + " requested)");
        }
        balance.setQuantityOnHand(newQuantity);
        balanceRepository.saveAndFlush(balance);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventoryItem(request.getInventoryItem());
        transaction.setWarehouse(request.getWarehouse());
        transaction.setSubLocation(request.getSubLocation());
        transaction.setLotNumber(request.getLotNumber());
        transaction.setExpiryDate(balance.getExpiryDate());
        transaction.setTransactionType(InventoryTransactionType.ADJUSTMENT);
        transaction.setQuantity(request.getQuantityDelta().abs());
        transaction.setReasonCode(request.getReason());
        var actorDetails = currentUserProvider.current();
        transaction.setPerformedByUserId(actorDetails.id());
        transaction.setPerformedByUsername(actorDetails.username());
        transaction = transactionRepository.save(transaction);

        request.setStatus(LifecycleRequestStatus.APPROVED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request.setResultingTransactionId(transaction.getId());
        request.setUpdatedBy(actor);
        return requestRepository.saveAndFlush(request);
    }

    @Transactional
    public InventoryManualAdjustmentRequest reject(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ValidationFailedException.singleField("reason", "A reason is required to reject an adjustment request");
        }
        InventoryManualAdjustmentRequest request = requireStatus(id, LifecycleRequestStatus.PENDING);
        UUID actor = currentUserProvider.current().id();
        requireIsRoutedApprover(request, actor);

        request.setStatus(LifecycleRequestStatus.REJECTED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request.setRejectionReason(reason);
        request.setUpdatedBy(actor);
        return requestRepository.saveAndFlush(request);
    }

    private void requireIsRoutedApprover(InventoryManualAdjustmentRequest request, UUID actor) {
        UUID approver = request.getEffectiveApproverId() != null
                ? request.getEffectiveApproverId()
                : routingService.resolveEffectiveApprover(request.getNominalApproverId());
        if (!approver.equals(actor)) {
            throw new AccessDeniedException("Only this adjustment request's routed approver may act on it");
        }
    }

    private InventoryManualAdjustmentRequest requireStatus(UUID id, LifecycleRequestStatus expected) {
        InventoryManualAdjustmentRequest request = get(id);
        if (request.getStatus() != expected) {
            throw new ConflictException("ADJUSTMENT_REQUEST_WRONG_STATUS",
                    "Adjustment request must be " + expected + "; current status is " + request.getStatus());
        }
        return request;
    }
}
