package com.iams.inventory.api.dto;

import java.util.UUID;

public record VendorResponse(
        UUID id,
        long version,
        String name,
        String contactEmail,
        String contactPhone,
        boolean active
) {
}
