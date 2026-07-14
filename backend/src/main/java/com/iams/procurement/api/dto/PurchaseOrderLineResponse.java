package com.iams.procurement.api.dto;

import com.iams.procurement.domain.PurchaseOrderLineStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderLineResponse(
        UUID id,
        long version,
        UUID purchaseOrderId,
        String description,
        int quantityOrdered,
        int quantityReceived,
        int quantityReturned,
        BigDecimal unitCost,
        PurchaseOrderLineStatus status
) {
}
