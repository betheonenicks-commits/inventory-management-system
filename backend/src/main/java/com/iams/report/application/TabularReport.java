package com.iams.report.application;

import java.time.Instant;
import java.util.List;

/**
 * EPIC-RPT's uniform report shape: every report is a titled table of string
 * cells. One shape means one generic CSV exporter (US-RPT-12), one generic
 * frontend renderer, and no per-report serialization code - a deliberate
 * trade of per-report richness for a backbone every future report (and the
 * eventual Excel/PDF exporters) plugs into unchanged.
 */
public record TabularReport(
        String key,
        String title,
        Instant generatedAt,
        List<String> columns,
        List<List<String>> rows
) {
}
