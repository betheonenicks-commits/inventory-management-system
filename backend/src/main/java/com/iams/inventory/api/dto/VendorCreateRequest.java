package com.iams.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;

public record VendorCreateRequest(
        @NotBlank String name,
        String contactEmail,
        String contactPhone
) {
}
