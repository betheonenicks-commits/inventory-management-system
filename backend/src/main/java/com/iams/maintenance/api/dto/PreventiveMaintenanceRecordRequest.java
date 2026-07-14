package com.iams.maintenance.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PreventiveMaintenanceRecordRequest(@NotNull UUID scheduleId, String notes, BigDecimal cost) {
}
