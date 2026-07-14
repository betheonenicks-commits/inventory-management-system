package com.iams.maintenance.application;

import java.math.BigDecimal;
import java.time.LocalDate;

/** US-LIF-06: close a repair event on the asset's return. */
public record CloseRepairCommand(LocalDate actualReturnDate, BigDecimal actualCost) {
}
