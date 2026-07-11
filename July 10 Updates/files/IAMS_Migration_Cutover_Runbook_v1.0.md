# Migration Cutover Runbook
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-MIG-RUNBOOK-1.0 | **Status:** Ready for Use | **Date:** 2026-07-10
**Fulfills:** FR-MIG-05 ("migration cutover runbook: rehearsal import on a staging copy, agreed acceptance thresholds, and a documented rollback step are required before production cutover"). This document is the runbook artifact itself — **executing it is a separate, later activity** that requires a staging environment and the organization's real legacy data, neither of which exist yet. Producing this runbook satisfies the documentation half of FR-MIG-05; the rehearsal-execution half is a pre-production-cutover task, not a pre-Sprint-1 one (see RTM Report v1.1 §6, gate item 5).

---

## 1. Scope and Preconditions

This runbook governs moving an organization's existing spreadsheet/legacy asset, person, vendor, and inventory-item data into IAMS via the bulk-import path (FR-MIG-01/03/04, US-MIG-01/03/04, API §3.13/§4.3). It assumes:

- R1 is deployed and stable in a **staging environment** that is a production-equivalent snapshot (SRS 2.0 §2.6).
- The organization has designated a **Migration Owner** (typically the Inventory Manager or Super Administrator) with authority to accept or reject the rehearsal outcome.
- Source data has been extracted from the legacy system(s) into the IAMS-provided CSV/Excel templates (FR-MIG-01) — this runbook does not cover source-side extraction or cleansing, which is organization-specific.

## 2. Roles

| Role | Responsibility |
|---|---|
| Migration Owner | Accepts/rejects rehearsal outcome against thresholds (§4); authorizes production cutover; sole authority to invoke rollback |
| Super Administrator | Executes the import jobs (dry-run and commit) in both staging and production |
| IT/Infrastructure Team | Provisions the staging environment; performs the pre-cutover backup; executes restore if rollback is invoked |
| Data Protection/Compliance Officer | Confirms migrated personal data (Person records) is in scope of the retention/PII configuration before any production commit |

## 3. Cutover Sequence

### Phase 1 — Staging Rehearsal (required, non-negotiable per FR-MIG-05)
1. Deploy the release candidate to staging, configured identically to the intended production environment (org hierarchy, categories, custom fields, roles already set up per the onboarding checklist — migration imports *data*, not *configuration*).
2. Load the full production-candidate dataset (not a sample) into the staging import templates.
3. Run **dry-run validation** (FR-MIG-03) against the full dataset. Do not proceed to commit until the dry-run's error report has been reviewed by the Migration Owner.
4. For every rejected row, classify the cause: (a) source-data defect — fix at source and re-run dry-run, or (b) template/mapping defect — file a defect against the import template, do not work around it by hand-editing individual rows.
5. Once the error rate stabilizes (i.e., re-running dry-run no longer reduces the rejection count), **commit** the staging import (FR-MIG-03, idempotency-keyed).
6. Record the reconciliation report (created / updated / rejected counts, generated identifiers) as the rehearsal's official result.
7. Spot-check a statistically meaningful sample of committed records against source data for silent transformation errors (e.g., date parsing, currency, category mapping) that wouldn't show up as a rejected row.

### Phase 2 — Go/No-Go Decision
The Migration Owner compares the rehearsal's reconciliation report against the acceptance thresholds in §4. This is a documented decision, not a verbal one — record it in the import run history's notes (FR-MIG-04) or an attached decision memo.

- **Go:** proceed to Phase 3.
- **No-Go:** return to source-data remediation, re-run Phase 1. Do not proceed to production with a rehearsal that failed its thresholds "because we're out of time" — that is exactly the failure mode this runbook exists to prevent.

### Phase 3 — Pre-Cutover Backup (mandatory, before any production write)
1. IT/Infrastructure Team takes a full backup of the production database **and** object store, per SRS 2.0 NFR-AVAIL-02/04 (the pair, not the database alone — the same object-store gap this baseline closed elsewhere, N-04, applies here too: don't let a migration-time backup silently skip MinIO).
2. Confirm the backup is restorable (not just "taken") — a dry restore-test to a scratch environment is strongly recommended before proceeding, consistent with NFR-DEPLOY-04's "documented pre-upgrade backup step."
3. Record the backup's identifier/timestamp in this runbook's execution log (§6) — this is what Phase 5 rollback restores *to*.

### Phase 4 — Production Cutover
1. Freeze conflicting production activity (no concurrent manual asset/person/vendor entry) for the cutover window — announce this window to all users in advance via the standard notification channel.
2. Run the same dry-run → review → commit sequence as Phase 1, against production, using the **same template version and mapping** validated in rehearsal. If the source dataset changed materially since rehearsal (e.g., more than a trivial delta), the rehearsal is stale — re-run Phase 1 against the updated dataset before proceeding.
3. Record the production reconciliation report.
4. Compare production results against rehearsal results — a large divergence (rejection rate, record counts) not explained by a known source-data delta is a signal to pause and investigate before unfreezing production activity.
5. Unfreeze production activity once the Migration Owner confirms acceptance.

### Phase 5 — Rollback (only if Phase 4 fails acceptance)
1. Migration Owner invokes rollback — this is the only path back, there is no "undo" for a partial commit.
2. IT/Infrastructure Team restores the database + object store from the Phase 3 backup.
3. Confirm restored state matches the pre-cutover backup (spot-check known records).
4. Root-cause the Phase 4 failure before attempting cutover again; do not simply retry immediately.

## 4. Acceptance Thresholds (fill in before first rehearsal; these are placeholders, not defaults)

| Metric | Threshold | Rationale |
|---|---|---|
| Row rejection rate (dry-run) | ≤ ____ % of total rows | Set based on known source-data quality; document why |
| Silent-transformation spot-check error rate | 0 tolerated on financial fields (purchase cost, currency); ≤ ____ % on descriptive fields | Financial figures feed depreciation/valuation reporting (FR-RPT-09) — errors there are not cosmetic |
| Rehearsal-to-production result divergence | ≤ ____ % unexplained | Anything above this pauses Phase 4 |
| Time to complete Phase 4 (production commit) | ≤ ____ hours | Should fit inside the announced freeze window |

*(The Migration Owner and Data Protection/Compliance Officer should set these numbers together before the first rehearsal — they depend on this organization's actual data quality, not a generic default. This runbook intentionally does not invent numbers on their behalf.)*

## 5. Rollback Decision Authority

Only the **Migration Owner** may invoke Phase 5 rollback. This is a deliberate single point of authority (contrast with the system's normal separation-of-duties model) because a cutover failure is a time-critical operational decision, not a routine approval — indecision during a failed cutover is itself a risk.

## 6. Execution Log (fill in at actual rehearsal/cutover time)

| Field | Rehearsal | Production |
|---|---|---|
| Date executed | | |
| Dataset version/source | | |
| Dry-run rejection count / rate | | |
| Commit reconciliation (created/updated/rejected) | | |
| Go/No-Go decision + who made it | | N/A |
| Pre-cutover backup ID/timestamp | N/A | |
| Rollback invoked? (Y/N) | N/A | |
| Final sign-off | | |
