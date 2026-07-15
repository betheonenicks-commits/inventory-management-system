package com.iams.procurement.application;

import java.util.List;
import java.util.UUID;

public record PurchaseOrderCreateCommand(UUID purchaseRequestId, String vendorName, UUID vendorId, List<PurchaseOrderLineCommand> lines) {
}
