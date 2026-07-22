package com.iams.migration.domain;

/**
 * US-MIG-03 lifecycle of an import run. A dry run lands in VALIDATED (the per-row
 * report is ready, nothing is written yet); an explicit commit moves it to
 * COMMITTED with reconciliation counts. FAILED is reserved for a run whose commit
 * itself could not proceed (e.g. the run was already committed under a different key).
 */
public enum ImportRunStatus {
    VALIDATED,
    COMMITTED,
    FAILED
}
