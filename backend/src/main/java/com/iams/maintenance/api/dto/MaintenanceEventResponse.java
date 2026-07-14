package com.iams.maintenance.api.dto;

import com.iams.maintenance.domain.MaintenanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MaintenanceEventResponse(
        UUID id,
        long version,
        UUID assetId,
        String assetNumber,
        UUID scheduleId,
        MaintenanceType maintenanceType,
        Instant performedAt,
        String notes,
        BigDecimal cost,
        UUID performedBy
) {
}
