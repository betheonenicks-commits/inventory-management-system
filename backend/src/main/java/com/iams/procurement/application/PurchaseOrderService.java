package com.iams.procurement.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.Vendor;
import com.iams.inventory.domain.VendorRepository;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.procurement.domain.PurchaseOrder;
import com.iams.procurement.domain.PurchaseOrderLine;
import com.iams.procurement.domain.PurchaseOrderLineEvent;
import com.iams.procurement.domain.PurchaseOrderLineEventRepository;
import com.iams.procurement.domain.PurchaseOrderLineEventType;
import com.iams.procurement.domain.PurchaseOrderLineRepository;
import com.iams.procurement.domain.PurchaseOrderLineStatus;
import com.iams.procurement.domain.PurchaseOrderRepository;
import com.iams.procurement.domain.PurchaseOrderStatus;
import com.iams.procurement.domain.PurchaseRequest;
import com.iams.procurement.domain.PurchaseRequestRepository;
import com.iams.procurement.domain.service.PurchaseOrderNumberGenerator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-LIF-02 (create, always linked to an approved request), US-LIF-03/16
 * (line-level receiving/reconciliation, pre-receipt cancellation, and
 * return-to-vendor - each producing its own {@link PurchaseOrderLineEvent}).
 */
@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderLineRepository lineRepository;
    private final PurchaseOrderLineEventRepository lineEventRepository;
    private final PurchaseRequestRepository requestRepository;
    private final PurchaseOrderNumberGenerator numberGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final VendorRepository vendorRepository;

    public PurchaseOrderService(PurchaseOrderRepository orderRepository, PurchaseOrderLineRepository lineRepository,
                                 PurchaseOrderLineEventRepository lineEventRepository, PurchaseRequestRepository requestRepository,
                                 PurchaseOrderNumberGenerator numberGenerator, CurrentUserProvider currentUserProvider,
                                 VendorRepository vendorRepository) {
        this.orderRepository = orderRepository;
        this.lineRepository = lineRepository;
        this.lineEventRepository = lineEventRepository;
        this.requestRepository = requestRepository;
        this.numberGenerator = numberGenerator;
        this.currentUserProvider = currentUserProvider;
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrderCreateCommand command) {
        if (command.lines() == null || command.lines().isEmpty()) {
            throw ValidationFailedException.singleField("lines", "At least one line item is required");
        }
        PurchaseRequest request = requestRepository.findById(command.purchaseRequestId())
                .orElseThrow(() -> NotFoundException.of("PurchaseRequest", command.purchaseRequestId()));
        if (request.getStatus() != LifecycleRequestStatus.APPROVED) {
            // AC-LIF-02-X: a PO without an approved request behind it is blocked.
            throw new ConflictException("PURCHASE_REQUEST_NOT_APPROVED",
                    "A purchase order can only be created from an approved purchase request");
        }
        for (PurchaseOrderLineCommand line : command.lines()) {
            if (line.quantityOrdered() <= 0) {
                throw ValidationFailedException.singleField("quantityOrdered", "Must be a positive quantity");
            }
        }

        UUID actor = currentUserProvider.current().id();
        PurchaseOrder order = new PurchaseOrder();
        order.setPoNumber(numberGenerator.next());
        order.setPurchaseRequest(request);
        order.setVendorName(command.vendorName());
        if (command.vendorId() != null) {
            // US-INV-07: an optional real link - a PO can still name a vendor in free text with none registered yet.
            Vendor vendor = vendorRepository.findById(command.vendorId())
                    .orElseThrow(() -> NotFoundException.of("Vendor", command.vendorId()));
            order.setVendor(vendor);
        }
        order.setStatus(PurchaseOrderStatus.OPEN);
        order.setCreatedBy(actor);
        order = orderRepository.save(order);

        for (PurchaseOrderLineCommand lineCommand : command.lines()) {
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setPurchaseOrder(order);
            line.setDescription(lineCommand.description());
            line.setQuantityOrdered(lineCommand.quantityOrdered());
            line.setUnitCost(lineCommand.unitCost());
            line.setStatus(PurchaseOrderLineStatus.OPEN);
            line.setCreatedBy(actor);
            lineRepository.save(line);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public PurchaseOrder get(UUID id) {
        return orderRepository.findByIdWithRequest(id).orElseThrow(() -> NotFoundException.of("PurchaseOrder", id));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> list(PurchaseOrderStatus status) {
        if (status != null) {
            return orderRepository.findByStatusWithRequestOrderByCreatedAtDesc(status);
        }
        return orderRepository.findAllWithRequestOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderLine> lines(UUID purchaseOrderId) {
        return lineRepository.findByPurchaseOrderIdOrderByCreatedAtAsc(purchaseOrderId);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderLineEvent> lineEvents(UUID lineId) {
        return lineEventRepository.findByLineIdOrderByCreatedAtAsc(lineId);
    }

    /** US-LIF-03/16: reconcile what physically arrived against this line - full or partial. */
    @Transactional
    public PurchaseOrderLine receive(UUID lineId, int quantity, String discrepancyNote) {
        if (quantity <= 0) {
            throw ValidationFailedException.singleField("quantity", "Must be a positive quantity");
        }
        PurchaseOrderLine line = getLine(lineId);
        if (line.getStatus() == PurchaseOrderLineStatus.CANCELLED) {
            throw new ConflictException("LINE_CANCELLED", "This line was cancelled and cannot receive stock");
        }
        if (line.getStatus() == PurchaseOrderLineStatus.FULLY_RECEIVED) {
            throw new ConflictException("LINE_ALREADY_FULLY_RECEIVED", "This line has already been fully received");
        }
        int newTotal = line.getQuantityReceived() + quantity;
        if (newTotal > line.getQuantityOrdered()) {
            // AC-LIF-03/16: over-receipt is a discrepancy the reconciliation must catch, not silently accept.
            throw new ConflictException("OVER_RECEIPT",
                    "Received quantity (" + newTotal + ") would exceed ordered quantity (" + line.getQuantityOrdered() + ")");
        }

        UUID actor = currentUserProvider.current().id();
        line.setQuantityReceived(newTotal);
        line.setStatus(newTotal == line.getQuantityOrdered() ? PurchaseOrderLineStatus.FULLY_RECEIVED : PurchaseOrderLineStatus.PARTIALLY_RECEIVED);
        line.setUpdatedBy(actor);
        line = lineRepository.saveAndFlush(line);
        appendEvent(line, PurchaseOrderLineEventType.RECEIVED, quantity, discrepancyNote, actor);

        closeOrderIfAllLinesSettled(line.getPurchaseOrder().getId());
        return line;
    }

    /** US-LIF-16: cancel a PO before any line has received stock. */
    @Transactional
    public PurchaseOrder cancel(UUID purchaseOrderId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ValidationFailedException.singleField("reason", "A reason is required to cancel a purchase order");
        }
        PurchaseOrder order = get(purchaseOrderId);
        if (order.getStatus() != PurchaseOrderStatus.OPEN) {
            throw new ConflictException("PURCHASE_ORDER_NOT_OPEN", "Only an open purchase order can be cancelled");
        }
        List<PurchaseOrderLine> orderLines = lines(purchaseOrderId);
        boolean anyReceived = orderLines.stream().anyMatch(l -> l.getQuantityReceived() > 0);
        if (anyReceived) {
            // AC-LIF-16: cancellation is only for "before receipt" - once anything has arrived, use return-to-vendor instead.
            throw new ConflictException("ALREADY_RECEIVED",
                    "This purchase order has lines with received stock and can no longer be cancelled outright");
        }

        UUID actor = currentUserProvider.current().id();
        for (PurchaseOrderLine line : orderLines) {
            line.setStatus(PurchaseOrderLineStatus.CANCELLED);
            line.setUpdatedBy(actor);
            lineRepository.saveAndFlush(line);
            appendEvent(line, PurchaseOrderLineEventType.CANCELLED, null, reason, actor);
        }

        order.setStatus(PurchaseOrderStatus.CANCELLED);
        order.setUpdatedBy(actor);
        return orderRepository.saveAndFlush(order);
    }

    /** US-LIF-16: log a return-to-vendor for previously received units found defective. */
    @Transactional
    public PurchaseOrderLine returnToVendor(UUID lineId, int quantity, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ValidationFailedException.singleField("reason", "A reason is required to log a vendor return");
        }
        if (quantity <= 0) {
            throw ValidationFailedException.singleField("quantity", "Must be a positive quantity");
        }
        PurchaseOrderLine line = getLine(lineId);
        int alreadyReturned = line.getQuantityReturned();
        int netReceived = line.getQuantityReceived() - alreadyReturned;
        if (quantity > netReceived) {
            throw new ConflictException("RETURN_EXCEEDS_RECEIVED",
                    "Cannot return " + quantity + " units - only " + netReceived + " received units are not already returned");
        }

        UUID actor = currentUserProvider.current().id();
        line.setQuantityReturned(alreadyReturned + quantity);
        line.setUpdatedBy(actor);
        line = lineRepository.saveAndFlush(line);
        appendEvent(line, PurchaseOrderLineEventType.RETURNED_TO_VENDOR, quantity, reason, actor);
        return line;
    }

    private void closeOrderIfAllLinesSettled(UUID purchaseOrderId) {
        List<PurchaseOrderLine> orderLines = lines(purchaseOrderId);
        boolean allSettled = orderLines.stream()
                .allMatch(l -> l.getStatus() == PurchaseOrderLineStatus.FULLY_RECEIVED || l.getStatus() == PurchaseOrderLineStatus.CANCELLED);
        if (allSettled) {
            PurchaseOrder order = get(purchaseOrderId);
            order.setStatus(PurchaseOrderStatus.CLOSED);
            order.setUpdatedBy(currentUserProvider.current().id());
            orderRepository.saveAndFlush(order);
        }
    }

    private void appendEvent(PurchaseOrderLine line, PurchaseOrderLineEventType type, Integer quantity, String note, UUID actor) {
        PurchaseOrderLineEvent event = new PurchaseOrderLineEvent();
        event.setLine(line);
        event.setEventType(type);
        event.setQuantity(quantity);
        event.setNote(note);
        event.setActorId(actor);
        lineEventRepository.save(event);
    }

    private PurchaseOrderLine getLine(UUID lineId) {
        return lineRepository.findByIdWithOrder(lineId).orElseThrow(() -> NotFoundException.of("PurchaseOrderLine", lineId));
    }
}
