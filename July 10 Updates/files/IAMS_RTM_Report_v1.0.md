# Requirements Traceability Matrix & Development-Readiness Report
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-RTM-REPORT-1.0 | **Status:** For Review | **Date:** 2026-07-10
**Built from:** `IAMS_RTM_and_Progress.xlsx` (the living RTM: 50 capability-level requirement rows, a 93-item Issues Log, a Daily Progress log 2026-07-04→2026-07-10, and a Readiness Summary), cross-walked against `IAMS_Epics_and_User_Stories_v1.0.md` (this session's 178-story backlog), and independently checked against every document in both `July 10 Updates/files/` **and** the older `files/` folder at the project root.

> **Why this report exists:** the xlsx is the authoritative data; it isn't easy to read or share as a status artifact, and it doesn't yet know about this session's Epic/User Story backlog or about three design documents sitting in the project's root `files/` folder. This report renders the xlsx, adds the missing Epic/User-Story column, and adds three findings the xlsx's own verdict didn't account for (§5). **Treat the xlsx as the system of record for day-to-day updates; treat §5 of this report as a correction/addition list to fold into it.**

---

## 1. Verdict, Up Front

**Conditionally development-ready for R1 Sprint 1 — three gating items, not the one the existing workbook shows.**

The existing Readiness Summary sheet states: *"Overall Verdict: READY — conditional on stakeholder ratification of consolidated baseline v2.0; UI sprints additionally await R1 wireframes (UX-01)."* That verdict is well-earned — 84 of 93 tracked issues are genuinely Resolved by the v2.0/1.1 baseline, and I independently spot-checked a sample of the "Resolved" claims (audit state machine, MinIO/object-store addition, offline-queue priority fix, break-glass, SoD waivers, ID reconciliation) against the actual FRS 2.0 / SRS 2.0 / DD 1.1 text and they hold up. But that verdict was scoped only to the eight documents in `July 10 Updates/files/`. It does not account for three "Draft for Engineering Implementation" design specs that already exist in the project's root `files/` folder and are not reconciled to the v2.0 ID baseline. See §5 for what that changes.

**Bottom line by track:**

| Track | Ready for Sprint 1? | Condition |
|---|---|---|
| **Backend/API (R1 scope)** | **Yes** | Stakeholder ratification sign-off of BRD 2.0/FRS 2.0/SRS 2.0/DD 1.1/AC-1.0 (the one blocking item the existing workbook names correctly) |
| **Frontend/UX (R1 scope)** | **Conditional** | The existing workbook says "blocked on UX-01 wireframes." In fact a full IA/nav/site-map spec (`Frontend-UX-Design-Specification.md`) already exists — but it cites the superseded BRD-4.0/FRS-4.0 ID scheme and contains at least one now-incorrect FR citation (§5, Finding A). It needs a short reconciliation pass, not a from-scratch wireframing effort. |
| **Architecture/DevOps** | **Yes, with the same reconciliation caveat** | `Backend-Architecture-Specification.md` and `Middleware-Infrastructure-Security-Specification.md` are substantially more detailed than SRS 2.0 §2 alone; same stale-citation caveat applies |
| **R2/R3 scope** | **Not yet** | By design — FRS 2.0 §4 gates R2 Must-Haves on Acceptance-Criteria stubs "before their first sprint," which haven't been written yet |

---

## 2. Traceability Matrix (BR → FR → Epic/User Story → Readiness)

Grain: one row per capability-level requirement, as tracked in the existing RTM workbook (`RTM` sheet). **Note on ID spaces:** these `BR-<MOD>-NN` IDs are the RTM's own tracking scheme from the original requirements-review process — a *different, more granular* numbering from BRD 2.0 §7's ratified `BR-01`…`BR-21` catalog. Both are legitimate; they've simply never been formally cross-walked to each other. Treat the FR and User-Story columns as the reliable cross-reference between the two.

Readiness values are carried from the workbook's `Readiness Status` column (all 50 rows currently read "Ready"); the **Note** column is this report's condensed restatement of each row's residual gap, corrected where the workbook itself was stale (flagged ⚠, see §5 Finding B).

### Asset Management (EPIC-AST)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-AST-01 | Register assets with unique identifier | FR-AST-01 | US-AST-01 | Ready |
| BR-AST-02 | Generate/print barcode and QR labels | FR-AST-02, FR-SCN-07 | US-AST-02, US-SCN-07 | Ready; label-stock matrix now explicit |
| BR-AST-03 | RFID-ready asset design | FR-AST-12, FR-SCN-06, FR-SRC-05 | US-AST-12, US-SCN-06, US-SRC-05 | Ready; scoped to extension point only, no R1 hardware |
| BR-AST-04 | Categories, groups, parent/child relationships | FR-AST-03, FR-AST-04 | US-AST-03, US-AST-04 | Ready; child disposition-on-transfer now defined |
| BR-AST-05 | Images and multiple attachments per asset | FR-AST-05 | US-AST-05 | Ready; upload validation NFR-SEC-10 closes the residual gap |
| BR-AST-06 | Custom fields per category | FR-AST-06 | US-AST-06 | Ready; JSONB + per-category JSON Schema |
| BR-AST-07 | Track location, custodian, status, condition, warranty | FR-AST-07, FR-AST-08, FR-AST-09, FR-LIF-04 | US-AST-07, US-AST-08, US-AST-09, US-LIF-04 | Ready |
| BR-AST-08 | Preserve historical ownership/location for audit traceability | FR-AST-10, FR-AST-11 | US-AST-10, US-AST-11 | Ready; append-only + optimistic locking mechanism now explicit |

### Inventory Management (EPIC-INV)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-INV-01 | Track stock quantities, consumables, spare parts | FR-INV-01, FR-INV-11 | US-INV-01, US-INV-11 | Ready; UoM added in FRS 2.0 |
| BR-INV-02 | Stock in/out transactions with traceability | FR-INV-02 | US-INV-02 | Ready; atomic row-level ops (NFR-CONC-02) |
| BR-INV-03 | Warehouse, shelf, bin management; inter-warehouse transfer | FR-INV-03, FR-INV-08 | US-INV-03, US-INV-08 | Ready |
| BR-INV-04 | Reorder levels and low-stock alerts | FR-INV-04, FR-NTF-06 | US-INV-04, US-NTF-06 | Ready; trigger catalog now parameterized |
| BR-INV-05 | Vendor management and purchase history | FR-INV-07 | US-INV-07 | Ready |
| BR-INV-06 | Physical count reconciliation and variance recording | FR-INV-05 | US-INV-05 | **Partially Resolved** — approver defined; numeric variance/write-off thresholds still a deployment-configuration item, not a system default. Non-blocking for build; needs an onboarding-checklist entry. |

### Organization Management (EPIC-ORG)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-ORG-01 | Hierarchy: campus/building/floor/room/dept/cost center (+ classroom/lab) | FR-ORG-01, FR-ORG-02, FR-ORG-05, FR-ORG-06 | US-ORG-01, US-ORG-02, US-ORG-05, US-ORG-06 | Ready |
| BR-ORG-02 | Associate assets/stock with org units; hierarchical reporting | FR-ORG-03, FR-ORG-04 | US-ORG-03, US-ORG-04 | Ready; Person split from `user_account` (DD 1.1 §B.1) |

### Asset Lifecycle (EPIC-LIF)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-LIF-01 | Purchase request and purchase order workflows | FR-LIF-01, FR-LIF-02 | US-LIF-01, US-LIF-02 | Ready |
| BR-LIF-02 | Receiving and assignment workflows | FR-LIF-03, FR-LIF-04, FR-LIF-16 | US-LIF-03, US-LIF-04, US-LIF-16 | Ready; partial-receipt/cancellation/return-to-vendor now specified |
| BR-LIF-03 | Transfer with approval trail | FR-LIF-05 | US-LIF-05 | Ready |
| BR-LIF-04 | Repair and maintenance scheduling; excluded from assignment while in repair | FR-LIF-06, FR-LIF-07, FR-LIF-08 | US-LIF-06, US-LIF-07, US-LIF-08 | Ready; maintenance-due trigger now in FR-NTF-06 catalog |
| BR-LIF-05 | Retirement/disposal/donation with reason, approver, date | FR-LIF-09, FR-LIF-11, FR-LIF-12 | US-LIF-09, US-LIF-11, US-LIF-12 | Ready |

### Audit Management (EPIC-AUD) — Core Differentiator
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-AUD-01 | Create quarterly/annual/surprise/department/room audits | FR-AUD-01, FR-AUD-03 | US-AUD-01, US-AUD-03 | Ready |
| BR-AUD-02 | Mobile & batch scanning; expected-vs-verified tracking | FR-AUD-04, FR-AUD-05, FR-AUD-06, FR-AUD-07, FR-AUD-08, FR-AUD-19 | US-AUD-04–08, US-AUD-19 | Ready; offline queue raised to **Must Have** (was the single largest priority/NFR mismatch found, N-10) |
| BR-AUD-03 | Missing/damaged reporting, condition tracking, photo evidence, remarks | FR-AUD-09, FR-AUD-10, FR-AUD-11, FR-AUD-12 | US-AUD-09–12 | Ready; checksummed evidence, backend-brokered access |
| BR-AUD-04 | Digital signature and completion certificate; immutable after completion | FR-AUD-12–15, FR-AUD-18 | US-AUD-13–16 | Ready; signature type clarified as electronic (typed name + re-auth), not cryptographic |
| BR-AUD-05 | Reconcile previously-missing assets with linked resolution | FR-AUD-21 | US-AUD-21 | Ready; append-only linked-record model, feeds BO-2 analytics per BRD §1.3.1 |
| BR-AUD-06 | Audit progress dashboard; reassign auditors; overdue flagging | FR-AUD-02, FR-DSH-02 | US-AUD-02, US-DSH-02 | Ready; dashboard staleness now bounded (≤30s for audit widget) |
| — | *(New in v2.0, not yet a numbered RTM row)* SoD conflict at submission; mid-audit scope change; statistical sampling | FR-AUD-20, FR-AUD-22, FR-AUD-23 | US-AUD-20, US-AUD-22, US-AUD-23 | **Add to RTM** — these three FRs post-date the original 50-row RTM baseline and have no BR row yet. Functionally covered; a tracking gap only. |

### Scanning (EPIC-SCN)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-SCN-01 | USB/Bluetooth/camera/webcam scanning; continuous mode; duplicate detection | FR-SCN-01, FR-SCN-02, FR-SCN-03, FR-SCN-04, FR-SCN-05 | US-SCN-01–05 | Ready |
| BR-SCN-02 | Unrecognized-scan flagging; missing/damaged label remediation | (exception paths of FR-SCN-01–05) | US-SCN-01–05 (exception ACs), US-SRC-02 | Ready; "register this asset?" affordance now specified (AC-SRC-02-X) |

### Reporting (EPIC-RPT) & Dashboard (EPIC-DSH)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-RPT-01 | 15 report types | FR-RPT-01–15 | US-RPT-01–15 | Ready; depreciation report now has a computable basis (FR-AST-16) |
| BR-RPT-02 | Export PDF/Excel/CSV; scheduled reports; role-based access; background export | FR-RPT-12, FR-RPT-13, FR-RPT-14 | US-RPT-12, US-RPT-13, US-RPT-14 | Ready; uniform async Job pattern (API §8) |
| BR-DSH-01 | KPIs: totals, breakdowns, audit %, expirations, low stock, activity, calendar | FR-DSH-01–07 | US-DSH-01–07 | Ready |

### User Management & RBAC (EPIC-USR)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-USR-01 | 8(→9) roles; permission by module/action; org-scoped access | FR-USR-01, FR-USR-02, FR-USR-03, FR-USR-04 | US-USR-01–04 | Ready; role count corrected to 9 defaults + 2 system-provided custom roles + Integration Service (FR-USR-01) |
| BR-USR-02 | Offboarding blocks deactivation until assigned assets recovered | ⚠ **FR-USR-08** (workbook row currently cites stale `FR-USR-05`) | US-USR-08 | Ready in substance — **but see §5 Finding B: fix the FR-ID cell in the live workbook**, it currently points at the wrong (renumbered) requirement |

### Notifications (EPIC-NTF)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-NTF-01 | Email/SMS(optional)/in-app for audits, warranty, maintenance, low stock, approvals | FR-NTF-01, FR-NTF-02, FR-NTF-03, FR-NTF-04, FR-NTF-06 | US-NTF-01–04, US-NTF-06 | Ready; was the weakest module pre-baseline (N-13), now has a full trigger catalog, recipient resolution, delivery tracking, templates, deep links |
| BR-NTF-02 | Per-user notification preferences with admin-mandatory types | FR-NTF-05 | US-NTF-05 | Ready |

### Search (EPIC-SRC)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-SRC-01 | Global + field-specific search | FR-SRC-01, FR-SRC-02, FR-SRC-03 | US-SRC-01–03 | Ready; backed by PostgreSQL full-text search, GIN indexes (DD 1.1 §C.4) |

### Security (EPIC-SEC)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-SEC-01 | JWT auth; AD/LDAP; RBAC; optional 2FA; optional IP restrictions | FR-SEC-01, FR-SEC-02, FR-SEC-03a/b, FR-SEC-07 | US-SEC-01–03, US-SEC-07 | Ready |
| BR-SEC-02 | Complete audit log; login history; suspicious-login investigation | FR-SEC-04, FR-SEC-09 | US-SEC-04, US-SEC-09 | Ready |
| BR-SEC-03 | Break-glass emergency access override (logged) | FR-SEC-16 | US-SEC-16 | Ready; this was previously **undefined scope hidden in a roles matrix** (issue SEC-05) — now a fully specified, time-boxed, dual-notified control |

### Data Migration (EPIC-MIG)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-MIG-01 | Bulk import with template, dry-run validation, commit, reconciliation report | FR-MIG-01, FR-MIG-03, FR-MIG-04, FR-MIG-05 | US-MIG-01, US-MIG-03, US-MIG-04, US-MIG-05 | Ready; cutover runbook (rehearsal + rollback) now a named process requirement |

### External Integrations (EPIC-INT)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-INT-01 | Accounting/depreciation export; integration enable/disable with joint approval | FR-INT-01, FR-INT-05 | US-INT-01, US-INT-05 | Ready; the BRD's own prior in/out-of-scope contradiction on accounting export is resolved (read-only export, R3, in scope) |

### Compliance & Privacy (EPIC-CMP)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-CMP-01 | Retention engine, anonymization with pseudonymous reference, legal hold | FR-CMP-01, FR-CMP-02, FR-CMP-06 | US-CMP-01, US-CMP-02, US-CMP-06 | Ready; named regulatory postures (GDPR/DPDP/FERPA-aligned) now in BRD 2.0 §9 |

### Product Analytics (EPIC-ANL)
| BR ID | Requirement | FR ID(s) | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-ANL-01 | Feature-usage reporting and in-app feedback | FR-ANL-03, FR-ANL-04 | US-ANL-03, US-ANL-04 | Ready; FR-ANL-02 (never-transmit guarantee) is the load-bearing companion FR — see US-ANL-02 |

### Non-Functional Requirements (EPIC-PLAT)
| BR ID | Requirement | SRS Ref | User Stor(y/ies) | Note |
|---|---|---|---|---|
| BR-NFR-01 | Availability target | NFR-AVAIL-01 | US-PLAT-03 | Ready; single reconciled figure — 99.5% during defined operating hours (was two conflicting figures) |
| BR-NFR-02 | Performance: standard ops / reports / scan resolution | NFR-PERF-01–05 | US-PLAT-01 (general), US-SCN-05 | Ready; p95 targets now quantified against stated dataset sizes |
| BR-NFR-03 | Scale: 100k assets; 100 concurrent users | NFR-SCALE-01–04 | US-PLAT-04, US-PLAT-05 | Ready; audit-day burst throughput now quantified (25 scans/sec, NFR-SCALE-04) |
| BR-NFR-04 | On-prem deployment; PostgreSQL | SRS §2.4, §7 | US-PLAT-01, US-PLAT-15 | Ready; single-tenant-per-deployment confirmed, no `tenant_id` |
| BR-NFR-05 | Backup/restore; audit-log retention; encryption | NFR-AVAIL-02/04/06, NFR-SEC-03 | US-PLAT-03 | Ready; object-store (MinIO) now included in backup/RPO/RTO scope — this was the single largest architecture gap found (N-04), since evidence photos are the product's compliance core |
| BR-NFR-06 | Offline/intermittent-connectivity operation for field scanning | NFR-AVAIL-03, NFR-SEC-12 | US-AUD-19, US-PLAT-02 | Ready; was "architecture-defining and entirely unspecified" (DOC-05) pre-baseline — now a fully specified durable client queue with idempotency-keyed sync |

---

## 3. Requirement-Baseline Issue Closure (from the Issues Log)

The workbook's Issues Log tracks 93 items opened across three review passes (2026-07-04, 2026-07-09 AM, 2026-07-09 PM) and re-reviewed against the v2.0 baseline on 2026-07-10.

| Status | Count | % |
|---|---|---|
| Resolved | 84 | 90% |
| Partially Resolved | 8 | 9% |
| Open | 1 | 1% |
| **Total** | **93** | |

**The 1 Open item:** `UX-01` — *"No screens, wireframes, flows, or navigation model."* Addressed in this report's §5 Finding A: partially closed by a document the workbook doesn't count.

**The 8 Partially Resolved items** are, without exception, deployment-configuration or backlog-level residuals — none block writing code:
- `BFR-09` — variance/write-off approval *thresholds* (approver role is defined; the numeric threshold is an onboarding-time configuration choice)
- `UX-02` / `UX-03` — UI copy/validation-message catalogs (field-level rules exist in API/DD; presentation copy waits on wireframes)
- `DM-05` — photo EXIF/PII scrubbing (schema-level PII tagging is done; EXIF scrubbing is a flagged backlog decision)
- `APP-05` — a *consolidated* configuration catalog (every individual configurable is specified; nobody has yet compiled the one-page list)
- `INT-02` — LDAP group→role mapping table (auth flows are complete; the mapping table is a security-sprint task, not a spec gap)
- `INT-04` — concrete SMS provider + per-country regulatory registration (the integration point and graceful-degradation behavior are specified; provider selection is a deployment decision)
- `SEC-06` — storage-level WORM enforcement (checksums + append-only + brokered access are done; WORM is optional hardening, explicitly deferred)

None of these require a requirements decision before Sprint 1 — they're implementation-time or onboarding-time choices that the baseline correctly left open.

---

## 4. What Actually Closed the Big Risks

Worth naming explicitly, since these were flagged as the highest-severity findings in the pre-baseline reviews and are exactly the kind of gap that would have caused mid-sprint rework if missed:

1. **N-04 (Architecture, High):** MinIO object store was mandatory per the Data Dictionary and API spec but absent from the SRS's deployment architecture and backup scope. Audit evidence photos — the product's core differentiator — would have sat outside backup/DR entirely. **Closed:** SRS 2.0 §2.4 adds the object-store container; NFR-AVAIL-02/04 extend backup/RPO/RTO to cover it; NFR-AVAIL-06 adds crash-consistency ordering (object write, then metadata commit).
2. **BFR-07 / N-05 (Data model, Critical/High):** the audit state machine had two contradicting versions across documents (Data Dictionary vs. API spec), and ~10 entities the API required didn't exist in the schema. **Closed:** DD 1.1 §A.1 aligns the enum to the API's state machine; §B adds all ten missing entities.
3. **N-10 (Offline capability, High):** the offline scan queue — this product's signature field-reliability promise — was only a **Should Have** while the NFR *guaranteed* no scan loss. An unenforceable guarantee. **Closed:** FR-AUD-19 raised to **Must Have**, with durability, visible queue depth, and idempotency-keyed sync specified.
4. **N-02 (Traceability, Critical):** five requirement IDs meant different things in different documents (e.g., `FR-USR-05` meant both "offboarding" and "System Operator scoping" depending which doc you read). **Closed:** FRS 2.0 Appendix B is now the authoritative old→new ID map — **except the one cell in the live RTM workbook this report catches in Finding B below, which still uses the pre-fix ID.**

---

## 5. Findings This Review Adds (Beyond the Existing Workbook)

### Finding A — Three "Draft for Engineering Implementation" specs exist outside the reviewed package and are not reconciled to v2.0

The project's root `files/` folder (not `July 10 Updates/files/`) contains three substantial, implementation-grade design documents that the existing RTM never evaluated:

| Document | Doc ID | Status marked | Cites |
|---|---|---|---|
| `Frontend-UX-Design-Specification.md` | IAMS-FUX-1.0 | Draft for Engineering Review | BRD-4.0, FRS-4.0, SRS-4.0, PUC-1.0 |
| `Backend-Architecture-Specification.md` | IAMS-BAS-1.0 | Draft for Engineering Implementation | BRD-4.0, FRS-4.0, SRS-4.0, PUC-1.0 |
| `Middleware-Infrastructure-Security-Specification.md` | IAMS-MIS-1.0 | Draft for Engineering Build | BRD-4.0, FRS-4.0, SRS-4.0, PUC-1.0 |

Two things are both true at once:

- **These are real, valuable assets.** The Frontend/UX spec alone contains a full information-architecture split (Admin/Desktop shell vs. Audit/Mobile shell), a role-by-role navigation matrix, and a complete route-level site map — exactly what the Issues Log's `UX-01` ("no screens, wireframes, flows, or navigation model") asks for. It is not literal pixel wireframes, but it is a legitimate, sprint-usable substitute for a screen inventory. Marking `UX-01` fully "Open" understates where the project actually is.
- **They cite a baseline that was never ratified and has since been superseded.** `BRD-4.0`, `FRS-4.0`, `SRS-4.0` are exactly the phantom versions Issue `N-01` found never existed as real documents — the ratified consolidation is BRD 2.0/FRS 2.0/SRS 2.0. Worse, at least one specific citation is now **wrong**, not just outdated: the Frontend/UX spec's route table (line 140) labels `/admin/compliance/waivers` as `FR-USR-07` ("SoD waivers"). Under the ratified FRS 2.0 Appendix B, SoD waivers were renumbered to **FR-USR-09**; `FR-USR-07` now means something unrelated — "flat, non-inheriting role model." An engineer implementing straight from this spec without cross-checking would tag the waivers screen with the wrong requirement ID.

**Recommendation:** before frontend/architecture work starts building against these three documents, run the same kind of reconciliation pass DD 1.0→1.1, API 1.0→1.1, and PUC 1.0→1.1 already went through — republish them as FUX-1.1 / BAS-1.1 / MIS-1.1 re-pointed at the v2.0/1.1 baseline, with a scripted ID cross-reference check (the same method Issues N-01/N-02 used). This is a document-reconciliation task, not a redesign — the content held up well against a spot-check of the FRS 2.0 requirements it claims to satisfy.

### Finding B — One stale FR-ID citation live in the RTM workbook itself

`RTM` sheet, row for `BR-USR-02` ("Offboarding blocks deactivation until assigned assets recovered") lists **`FR-USR-05`** in the "Referenced FRS ID(s)" column, with "FRS ID Resolvable? = Yes — resolvable in FRS 2.0 (Appendix B reconciled)." That's the *pre-reconciliation* ID. Under the ratified FRS 2.0, `FR-USR-05` means "System Operator scoped to technical configuration only" — a different requirement entirely. The correct citation for offboarding is **`FR-USR-08`** (confirmed against FRS 2.0 §2.9 and Appendix B, and against DD 1.1 §A.3's `pending_asset_review` field, which the amendment explicitly notes was "corrected to FR-USR-08, was draft FR-USR-05"). One-cell fix in the workbook; flagged here so it doesn't propagate into a test-case ID or a sprint ticket.

### Finding C — Two independent BR numbering schemes are in play, unreconciled

The RTM workbook's `BR-<MODULE>-NN` IDs (e.g., `BR-AST-01`) are a capability-tracking scheme from the original requirements-review process. BRD 2.0 §7's `BR-01`…`BR-21` is a separate, ratified catalog (e.g., `BR-01` = "maintain a complete, searchable register..."). Nothing is wrong with having both, but no document currently cross-walks them, so a stakeholder skimming both artifacts could reasonably assume `BR-AST-01` and `BR-01` are the same identifier space. Low risk, easy fix: a one-time appendix mapping the 50 RTM rows to their BRD 2.0 §7 parent BR.

---

## 6. Pre-Sprint-1 Gate Checklist

| # | Item | Owner | Status |
|---|---|---|---|
| 1 | Stakeholder ratification sign-off of BRD 2.0 / FRS 2.0 / SRS 2.0 / DD 1.1 / AC-1.0 as the single baseline | Executive Sponsor + module owners | **Open — the one gate the existing workbook names, still valid** |
| 2 | Reconcile `Frontend-UX-Design-Specification.md`, `Backend-Architecture-Specification.md`, `Middleware-Infrastructure-Security-Specification.md` to v2.0/1.1 IDs (republish as .1.1) | Whoever owns those three docs | **Open — new gate from this review (Finding A)** |
| 3 | Correct the `FR-USR-05`→`FR-USR-08` cell on `BR-USR-02` in the RTM workbook | RTM owner | **Open — trivial, new from this review (Finding B)** |
| 4 | Third-party penetration test scheduled ahead of first production go-live (FR-SEC-13) | IT Security Officer | Process requirement, not a Sprint-1 blocker, but should be scheduled now given lead time |
| 5 | Migration cutover rehearsal + rollback runbook (FR-MIG-05) | Super Administrator / Data owner | Needed before production cutover, not before Sprint 1 |
| 6 | R2 Must-Have Acceptance-Criteria stubs, per FRS 2.0 §4's gate | Product Management | Needed before R2's first sprint, not R1's |

**Nothing on this list blocks starting R1 backend engineering.** Items 2–3 block trusting the frontend/architecture specs *as delivered*; they don't block writing them, since the underlying FRS/API/DD contracts they need to point at are themselves solid.

---

## Appendix — Source Documents Consulted

`July 10 Updates/files/`: IAMS_BRD_v2.0.md, IAMS_FRS_v2.0.md, IAMS_SRS_v2.0.md, IAMS_Acceptance_Criteria_R1.md, IAMS_Data_Dictionary_v1.1_Amendment.md, IAMS_Personas_and_Use_Cases_v1.1.md, IAMS_API_Specification_v1.1.md, IAMS_RTM_and_Progress.xlsx (all 4 sheets), IAMS_Epics_and_User_Stories_v1.0.md (this session's companion artifact).
`files/` (project root): BRD.md (confirmed superseded draft, IAMS-BRD-3.0), Frontend-UX-Design-Specification.md, Backend-Architecture-Specification.md, Middleware-Infrastructure-Security-Specification.md, API-Interface-Specification.md (confirmed superseded draft, IAMS-API-1.0).
