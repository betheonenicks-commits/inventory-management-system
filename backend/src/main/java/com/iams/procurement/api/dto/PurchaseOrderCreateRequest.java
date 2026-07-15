package com.iams.procurement.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderCreateRequest(
        @NotNull UUID purchaseRequestId,
        @NotBlank String vendorName,
        UUID vendorId,
        @NotEmpty @Valid List<PurchaseOrderLineRequest> lines
) {
}
