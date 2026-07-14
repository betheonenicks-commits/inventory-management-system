package com.iams.procurement.application;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseRequestCreateCommand(
        String itemDescription,
        String justification,
        BigDecimal estimatedCost,
        String vendorName,
        UUID nominalApproverId
) {
}
