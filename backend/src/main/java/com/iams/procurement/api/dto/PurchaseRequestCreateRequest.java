package com.iams.procurement.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseRequestCreateRequest(
        @NotBlank String itemDescription,
        @NotBlank String justification,
        BigDecimal estimatedCost,
        String vendorName,
        @NotNull UUID nominalApproverId
) {
}
