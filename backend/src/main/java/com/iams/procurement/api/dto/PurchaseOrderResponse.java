package com.iams.procurement.api.dto;

import com.iams.procurement.domain.PurchaseOrderStatus;
import java.util.UUID;

public record PurchaseOrderResponse(
        UUID id,
        long version,
        String poNumber,
        UUID purchaseRequestId,
        String vendorName,
        UUID vendorId,
        PurchaseOrderStatus status
) {
}
