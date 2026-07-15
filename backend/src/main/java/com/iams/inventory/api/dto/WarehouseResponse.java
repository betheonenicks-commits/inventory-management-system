package com.iams.inventory.api.dto;

import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        long version,
        String name,
        String code,
        UUID orgNodeId,
        String orgNodeName,
        boolean active
) {
}
