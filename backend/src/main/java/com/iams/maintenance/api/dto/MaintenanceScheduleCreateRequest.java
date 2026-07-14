package com.iams.maintenance.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceScheduleCreateRequest(
        @NotNull UUID assetId,
        @NotNull Integer intervalMonths,
        @NotNull LocalDate nextDueDate,
        String description
) {
}
