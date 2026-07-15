package com.iams.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WarehouseCreateRequest(
        @NotBlank String name,
        @NotBlank String code,
        @NotNull UUID orgNodeId
) {
}
