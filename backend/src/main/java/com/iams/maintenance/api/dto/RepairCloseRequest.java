package com.iams.maintenance.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RepairCloseRequest(@NotNull LocalDate actualReturnDate, BigDecimal actualCost) {
}
