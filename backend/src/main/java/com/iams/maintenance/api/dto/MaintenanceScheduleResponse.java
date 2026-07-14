package com.iams.maintenance.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceScheduleResponse(
        UUID id,
        long version,
        UUID assetId,
        String assetNumber,
        int intervalMonths,
        LocalDate nextDueDate,
        String description,
        boolean active
) {
}
