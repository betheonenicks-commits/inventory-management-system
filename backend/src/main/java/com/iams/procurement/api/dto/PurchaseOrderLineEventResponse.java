package com.iams.procurement.api.dto;

import com.iams.procurement.domain.PurchaseOrderLineEventType;
import java.time.Instant;
import java.util.UUID;

public record PurchaseOrderLineEventResponse(
        UUID id,
        UUID lineId,
        PurchaseOrderLineEventType eventType,
        Integer quantity,
        String note,
        UUID actorId,
        Instant createdAt
) {
}
