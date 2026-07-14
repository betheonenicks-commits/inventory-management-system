package com.iams.maintenance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CorrectiveMaintenanceRecordRequest(@NotNull UUID assetId, @NotBlank String notes, BigDecimal cost) {
}
