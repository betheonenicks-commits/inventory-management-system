package com.iams.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;

public record WarehouseUpdateRequest(@NotBlank String name) {
}
