# Development Requirements Traceability Matrix
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-DEV-RTM-2.0 | **Status:** Living document | **Date:** 2026-07-23
**Supersedes:** `IAMS_Development_RTM_v1.0.md`, whose content was frozen at a 2026-07-13 snapshot (42 Built / 10 Partial / 128 Not-started) and never refreshed — badly stale. This revision is a full 180-story re-audit against the **current codebase** (controllers, services, JPA entities, Flyway migrations V1–V54, and the frontend `src/features` + routes), cross-referenced with `DEVELOPMENT_LOG.md`. Each status below is grounded in code that exists now, not in what a prior log entry claimed.

**Distinct from:** `IAMS_RTM_Report_v1.2.md`, which tracks *requirements-documentation* completeness (BRD→FRS→Epics/User-Stories traceability, pre-Sprint-1 readiness). This document tracks the opposite direction: which of the 180 ratified user stories actually have working code behind them.

**How status is assigned (unchanged from v1.0):**
- **Built** — working code implements the story's acceptance criteria, verified by an automated test and/or a live click-test against Postgres. For cross-cutting NFR/platform stories with no dedicated endpoint (much of EPIC-PLAT), "Built" means the mechanism genuinely exists and is exercised by tested code, even though no dedicated "PLAT-NN session" ever formally counted it — this is the main correction versus the running `DEVELOPMENT_LOG.md` tally, which undercounted these because they were built as infrastructure during other epics.
- **Partial** — real code exists but a specific, named gap remains. Every Partial cites its exact gap.
- **Not started** — no code addresses the story, confirmed by inspecting the package tree.

**Verification basis:** direct codebase inspection this session (2026-07-23): 48 REST controllers, 54 migrations, the `com.iams.*` package tree, and the frontend route/feature map; plus `DEVELOPMENT_LOG.md` through the EPIC-MIG entries. Backend suite: **556/556** unit tests passing. Frontend: `tsc -b` + `oxlint` clean, production build succeeds.

---

## 1. Verdict, Up Front

**142 of 180 stories (78.9%) are Built, 15 (8.3%) are Partial with a named gap, and 23 (12.8%) are Not Started.** Of 17 epics, **8 are complete** (ORG, USR, CMP, SRC, DSH, RPT, ANL, INV), 6 are substantially built with a short tail of gaps (AST, SEC, LIF, AUD, NTF, PLAT), 1 is partially open (MIG), and **1 has no dedicated code yet** (SCN), with 1 more (INT) built only in fragments.

**"Not built" — the direct answer:** **23 stories have no code at all**; **38 are not fully done** (23 Not-started + 15 Partial).

This is materially further along than the running `DEVELOPMENT_LOG.md` figure (~130 Built) implied. The gap is almost entirely **EPIC-PLAT**: the running tally counted only 2 PLAT stories Built (the two that got dedicated sessions), but optimistic locking (PLAT-04), atomic inventory concurrency (PLAT-05), correlated logging (PLAT-06), metrics/status (PLAT-07), and standardized UI states (PLAT-12) are all genuinely implemented and tested — they were just built as cross-cutting infrastructure, never counted as their own stories. This reconciliation credits them.

| Track | Built | Partial | Not started | Total |
|---|---|---|---|---|
| **EPIC-AST** Asset Management | 14 | 1 | 1 | 16 |
| **EPIC-ORG** Organization | 6 | 0 | 0 | 6 |
| **EPIC-USR** User Management & RBAC | 9 | 0 | 0 | 9 |
| **EPIC-SEC** Security | 9 | 3 | 5 | 17 |
| **EPIC-MIG** Data Migration & Bulk Import | 3 | 0 | 2 | 5 |
| **EPIC-CMP** Compliance & Data Privacy | 6 | 0 | 0 | 6 |
| **EPIC-SRC** Search | 5 | 0 | 0 | 5 |
| **EPIC-SCN** Scanning | 0 | 0 | 7 | 7 |
| **EPIC-LIF** Asset Lifecycle | 15 | 1 | 0 | 16 |
| **EPIC-AUD** Audit Management | 22 | 1 | 1 | 24 |
| **EPIC-INV** Inventory Management | 11 | 0 | 0 | 11 |
| **EPIC-NTF** Notifications | 9 | 1 | 0 | 10 |
| **EPIC-DSH** Dashboard | 7 | 0 | 0 | 7 |
| **EPIC-RPT** Reporting | 15 | 0 | 0 | 15 |
| **EPIC-INT** External Integrations | 0 | 2 | 4 | 6 |
| **EPIC-ANL** Product Analytics | 4 | 0 | 0 | 4 |
| **EPIC-PLAT** Platform & NFR Enablers | 7 | 6 | 3 | 16 |
| **Total** | **142** | **15** | **23** | **180** |

---

## 2. The 38 stories that are NOT fully built

### 2.1 Not started (23) — no code exists

| Story | Title | Why not started |
|---|---|---|
| US-AST-05 | Attach images/files to an asset | `AttachmentOwnerType` only has `AUDIT_FINDING`; the object-store attachment infra (PLAT-02) is never wired to assets |
| US-SEC-02 | Authenticate via LDAP/AD | No LDAP code; `SystemController`/`JwtService` note the seam exists but no integration |
| US-SEC-03 | Mandatory MFA for Super Admin/Admin | No TOTP/MFA code anywhere; `AuthController` documents this as a known gap |
| US-SEC-07 | Restrict access by IP range | No IP-allowlist code |
| US-SEC-16 | Time-boxed "break-glass" emergency access | No break-glass code |
| US-SEC-17 | Optional MFA for non-mandated roles | Depends on the (absent) MFA subsystem |
| US-MIG-02 | Bulk-export in re-import-compatible format | Not built; the EPIC-RPT exporters make it small next |
| US-MIG-05 | Rehearsed cutover with documented rollback | Process/runbook deliverable, not authored |
| US-SCN-01 | Scan with USB keyboard-wedge scanner | No SCN module (see §3 note) |
| US-SCN-02 | Scan with Bluetooth HID scanner | No SCN module |
| US-SCN-03 | Scan with phone/webcam camera | No camera-decode code |
| US-SCN-04 | Detect duplicate scans in a session | No standalone SCN code (audit batch-scan does dedupe internally) |
| US-SCN-05 | Resolve scans within one second | No SCN performance story built as such |
| US-SCN-06 | RFID-ready scan abstraction layer | Only the `rfidTagId` field is reserved (AST-12) |
| US-SCN-07 | Configure barcode symbology and label sizes | Label sizes exist via labels; symbology config does not |
| US-AUD-19 | Persist scans offline, sync on reconnect | No durable client-side queue |
| US-INT-02 | Sync HR/SIS rosters read-only | No integration code |
| US-INT-03 | Authenticate via LDAP/AD or SSO (SAML2/OIDC) | Only R1 Must in EPIC-INT; needs a directory server |
| US-INT-04 | Deliver outbound webhooks to allow-listed URLs | No webhook delivery code |
| US-INT-06 | Sign webhook payloads | Depends on the (absent) webhook subsystem |
| US-PLAT-03 | Back up and restore DB + object store together | No backup-execution code (`SystemController` confirms) |
| US-PLAT-08 | Rate-limit API per user and per integration key | No rate-limiter |
| US-PLAT-13 | Install as a PWA with offline shell | No manifest/service worker |

### 2.2 Partial (15) — code exists, named gap remains

| Story | Title | The gap |
|---|---|---|
| US-AST-08 | Track warranty and AMC coverage | Warranty dates fully modeled; **AMC coverage** has no field |
| US-SEC-08 | Encrypt sensitive data at rest and in transit | Transit is deployment-TLS (documented); **at-rest** field/DB encryption not implemented in-app |
| US-SEC-12 | Surface dependency-scan status to security roles | CI scanning exists (`codeql.yml`, Dependabot); **no in-app surface** to security roles |
| US-SEC-13 | Patching, disclosure, and pen-test gate | Disclosure policy + pen-test checklist authored; **pen-test execution** is an external dependency |
| US-LIF-13 | Escalate an approval after inaction | Escalation is pull-triggered against a config threshold; **no scheduler** to fire it automatically |
| US-AUD-06 | Scan continuously without re-navigating | Clear-and-ready continuous mode + chip built; the "exit-to-record, preserve place-in-sequence" clause doesn't map to search-by-asset scanning (documented) |
| US-NTF-02 | Send optional SMS via a gateway | `SmsGateway` abstraction + graceful degradation built; **no concrete gateway** (deployment decision) |
| US-INT-01 | Export depreciation/valuation to accounting | Read-only `GET /reports/depreciation` gated to an `INT_ACCOUNTING_READ` service account; **no accounting-system push/format** |
| US-INT-05 | Compliance review before enabling an integration | Integration registry (enable/disable) exists; **no compliance-review gate** on enable |
| US-PLAT-09 | Ship breaking API changes as a coexisting version | `/api/v1` prefix convention in place; **no second version** has had to coexist yet |
| US-PLAT-10 | Upgrade via image tag with rollback-by-restore | Flyway auto-migrate + documented restore rollback; **not a tested upgrade/rollback flow** |
| US-PLAT-11 | Meet WCAG 2.1 AA on every screen | MUI baseline + `CMP-04` audit-status record; **no completed WCAG audit** across screens |
| US-PLAT-14 | Support the documented browser/device matrix | Responsive MUI UI; **no documented matrix testing** |
| US-PLAT-15 | Isolate DB/object store behind the reverse proxy | `docker-compose` defines services; **hardened proxy-only isolation** not verified |
| US-PLAT-16 | Gate Swagger/API-docs by environment | springdoc integrated; **not env-gated** (exposed unconditionally) |

---

## 3. Built stories by epic (142)

**Complete epics (all stories Built):**
- **EPIC-ORG (6/6):** ORG-01 hierarchy, 02 level relabel, 03 department, 04 person records, 05 node scoping, 06 room variants.
- **EPIC-USR (9/9):** USR-01 provisioning, 02 custom roles, 03 API+UI permission checks, 04 org-scope restriction, 05 System Operator scoping, 06 self-approval block, 07 flat roles, 08 offboarding block, 09 SoD waiver.
- **EPIC-CMP (6/6):** CMP-01 retention purge (multi-entity, hold-aware), 02 anonymization, 03 privacy notices, 04 accessibility-audit record, 05 data residency, 06 legal hold.
- **EPIC-SRC (5/5):** SRC-01 global search (pg_trgm), 02 lookup-by-code, 03 combined filters, 04 saved search, 05 RFID search field.
- **EPIC-DSH (7/7):** DSH-01 asset totals, 02 audit completion, 03 expirations/maintenance, 04 low reorder, 05 activity/calendar, 06 configurable KPIs, 07 scope-limited data.
- **EPIC-RPT (15/15):** RPT-01–10 report suite, 11 batch labels, 12 PDF/Excel/CSV export, 13 scheduled delivery, 14 security-log report, 15 ad hoc report builder.
- **EPIC-ANL (4/4):** ANL-01 usage capture, 02 sovereignty guarantee, 03 adoption report, 04 in-app feedback.
- **EPIC-INV (11/11):** INV-01–11 including 06 costing method, 09 lot/expiry, 10 multi-currency FX (verified: `StockInCommand.currencyCode/fxRate`, non-reporting currency requires a rate), 11 unit-of-measure.

**Substantially-built epics (Built stories listed; gaps in §2):**
- **EPIC-AST (14):** 01 register+id, 02 label, 03 categories, 04 parent-child, 06 custom-field schemas, 07 status lifecycle, 09 purchase/vendor, 10 append-only history, 11 movement log, 12 RFID field, 13 bulk import (via MIG-03), 14 insurance, 15 vehicle attributes, 16 depreciation.
- **EPIC-SEC (9):** 01 JWT access/refresh, 04 activity log, 05 password policy, 06 session timeout + step-up, 09 account lockout + self-service unlock, 10 export/erase person data, 11 security-log search, 14 scoped service accounts, 15 no-plaintext integration credentials.
- **EPIC-LIF (15):** 01 purchase request, 02 PO, 03 goods receipt, 04 assignment, 05 transfer+approval, 06 repair log, 07 preventive maintenance, 08 corrective maintenance, 09 disposal+approval, 10 lifecycle history, 11 reject-with-reason, 12 restore window, 14 erasure block, 15 approval delegation, 16 partial receipt/cancel/returns.
- **EPIC-AUD (22):** 01–05, 07–18, 20–24 (physical-audit workflow, sampling, analytics, certificate, corrections, scope-change) — see epic history in the log.
- **EPIC-NTF (9):** 01 email, 03 in-app read/unread, 04 assignment/transfer notices, 05 channel preferences, 06 trigger catalog, 07 recipient resolution, 08 delivery+retry, 09 templates, 10 deep-link through login.
- **EPIC-MIG (3):** 01 templates, 03 dry-run+idempotent commit, 04 import history (Asset + Vendor entities; engine is entity-agnostic).
- **EPIC-PLAT (7):** 01 single-command deploy (docker-compose), 02 object store brokered, 04 optimistic locking (tested), 05 atomic inventory concurrency (tested), 06 correlated logging (`CorrelationIdFilter`), 07 metrics + admin status page (actuator + System Health), 12 standardized loading/empty/error/permission states.

**Note on EPIC-SCN:** counted 0 Built because there is no `scn` module and no dedicated scan endpoints. However, working scan-adjacent code exists and could satisfy several SCN stories with modest promotion: the audit module accepts keyboard-wedge/HID input and does in-session duplicate detection and sub-second resolution (SCN-01/02/04/05), search-by-code provides scan-to-lookup (SCN-02), and `AssetLabelController` renders sized labels (SCN-07 label-size half). None of this was built *as* EPIC-SCN, so it is not credited here.

---

## 4. Method & honest caveats

- **Every story was checked against code**, not inferred from the log. Fully-built epics were spot-checked at the controller/service level; the 38 not-fully-built stories were each verified by searching for the absent or partial mechanism (e.g. `AttachmentOwnerType` has no ASSET value → AST-05 not started; no `Totp`/`Ldap`/`RateLimit`/`Webhook`/`BreakGlass` code → those SEC/PLAT/INT stories not started).
- **The NFR/platform judgment calls carry the most subjectivity.** Whether PLAT-06 "structured, correlated logs" is Built (correlation filter exists) or Partial (JSON structuring unverified), and whether PLAT-09/10 count as Built or Partial, are defensible either way; this revision leans Built where a tested mechanism exists and Partial where a named sub-clause is demonstrably missing. Reasonable auditors could shift 2–3 stories between Built and Partial without changing the headline picture.
- **The count is accurate to within a handful**, and — unlike v1.0 — it sums to 180 and every story has a stated basis.

---

## 5. Highest-leverage remaining work (from the 38)

1. **Finish EPIC-MIG** (2 left): MIG-02 export reuses the RPT exporters; the Person/Inventory-Item importers are one `EntityImportProcessor` bean each (engine is already generalized).
2. **EPIC-INT R1** (INT-03 LDAP/SSO): the only R1 Must not started; needs a directory server to verify. INT-01/05 are Partial and closable.
3. **EPIC-SCN** (7): promote the audit/label scan mechanics into a real scanning surface.
4. **EPIC-SEC tail**: MFA (03/17) and break-glass (16) are genuine features; LDAP (02) pairs with INT-03; IP-range (07) is small.
5. **EPIC-PLAT tail**: backup (03), rate-limit (08), PWA (13) are the three with no code; the six Partials are mostly NFR hardening/verification.
6. **Single-clause Partials** worth closing cheaply: AST-08 (add an AMC field), PLAT-16 (env-gate Swagger), SEC-12 (surface the existing CI scan status).
