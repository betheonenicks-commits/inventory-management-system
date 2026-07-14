package com.iams.asset.application;

import com.iams.asset.domain.DepreciationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Either a computed schedule, or NOT_DEPRECIATED when nothing is configured
 * (category default and per-asset override both absent) - deliberately not a
 * misleading zero, per FR-RPT-09's stated intent.
 */
public record DepreciationResult(
        Status status,
        DepreciationMethod method,
        Integer usefulLifeMonths,
        BigDecimal salvageValue,
        BigDecimal monthlyDepreciation,
        BigDecimal accumulatedDepreciation,
        BigDecimal netBookValue,
        LocalDate asOf
) {
    public enum Status {
        COMPUTED,
        NOT_DEPRECIATED
    }

    public static DepreciationResult notDepreciated(LocalDate asOf) {
        return new DepreciationResult(Status.NOT_DEPRECIATED, null, null, null, null, null, null, asOf);
    }
}
