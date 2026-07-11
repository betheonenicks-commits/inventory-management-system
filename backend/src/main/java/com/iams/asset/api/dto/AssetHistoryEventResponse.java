package com.iams.asset.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AssetHistoryEventResponse(
        UUID id,
        String eventType,
        String fieldName,
        String oldValue,
        String newValue,
        UUID correctionOfEventId,
        UUID createdBy,
        Instant createdAt
) {
}
