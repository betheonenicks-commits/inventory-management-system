package com.iams.procurement.api;

import com.iams.procurement.api.dto.PurchaseOrderLineEventResponse;
import com.iams.procurement.api.dto.PurchaseOrderLineResponse;
import com.iams.procurement.api.dto.PurchaseOrderResponse;
import com.iams.procurement.api.dto.PurchaseRequestResponse;
import com.iams.procurement.domain.PurchaseOrder;
import com.iams.procurement.domain.PurchaseOrderLine;
import com.iams.procurement.domain.PurchaseOrderLineEvent;
import com.iams.procurement.domain.PurchaseRequest;
import org.springframework.stereotype.Component;

@Component
public class ProcurementMapper {

    public PurchaseRequestResponse toResponse(PurchaseRequest request) {
        return new PurchaseRequestResponse(
                request.getId(),
                request.getVersion(),
                request.getItemDescription(),
                request.getJustification(),
                request.getEstimatedCost(),
                request.getVendorName(),
                request.getStatus(),
                request.getNominalApproverId(),
                request.getEffectiveApproverId(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getDecidedBy(),
                request.getDecidedAt(),
                request.getRejectionReason()
        );
    }

    public PurchaseOrderResponse toResponse(PurchaseOrder order) {
        return new PurchaseOrderResponse(
                order.getId(),
                order.getVersion(),
                order.getPoNumber(),
                order.getPurchaseRequest().getId(),
                order.getVendorName(),
                order.getStatus()
        );
    }

    public PurchaseOrderLineResponse toResponse(PurchaseOrderLine line) {
        return new PurchaseOrderLineResponse(
                line.getId(),
                line.getVersion(),
                line.getPurchaseOrder().getId(),
                line.getDescription(),
                line.getQuantityOrdered(),
                line.getQuantityReceived(),
                line.getQuantityReturned(),
                line.getUnitCost(),
                line.getStatus()
        );
    }

    public PurchaseOrderLineEventResponse toResponse(PurchaseOrderLineEvent event) {
        return new PurchaseOrderLineEventResponse(
                event.getId(),
                event.getLine().getId(),
                event.getEventType(),
                event.getQuantity(),
                event.getNote(),
                event.getActorId(),
                event.getCreatedAt()
        );
    }
}
