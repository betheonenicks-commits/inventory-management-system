package com.iams.procurement.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PurchaseOrderLineRequest(
        @NotBlank String description,
        @Positive int quantityOrdered,
        @NotNull BigDecimal unitCost
) {
}
