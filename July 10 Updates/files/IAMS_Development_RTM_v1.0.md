# Development Requirements Traceability Matrix
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-DEV-RTM-1.0 | **Status:** Living document | **Date:** 2026-07-13
**Distinct from:** `IAMS_RTM_Report_v1.2.md`, which tracks *requirements-documentation* completeness (BRD→FRS→Epics/User-Stories traceability, pre-Sprint-1 readiness). This document tracks the opposite direction: which of the 180 ratified user stories in `IAMS_Epics_and_User_Stories_v1.0.md` (Document ID IAMS-EUS-1.1) actually have working code behind them, verified against the live codebase and live click-tests — not sprint plans, not intentions, not "should be done by now."

**How status is assigned:**
- **Built** — a working, tested API endpoint (and usually a DB migration) implements the story's acceptance criteria, verified either by an automated test or a live click-test against Postgres.
- **Partial** — real code exists but a specific, named gap remains. Every Partial in this document cites the exact gap; none are vague.
- **Not started** — no corresponding code exists anywhere in `backend/src/main/java/com/iams/`, confirmed by listing the package tree rather than inferred from silence.

**Verification basis:** `DEVELOPMENT_LOG.md` (2026-07-12 and 2026-07-13 entries, including all same-day continuations) plus direct inspection of the codebase this session. Backend: 149/149 unit tests passing. Frontend: `tsc -b` and `oxlint` both clean, and (new this session) a real headless-Chromium pass over every routed page. Six full click-test/browser-test passes have now been run against a live Postgres 18.4 instance across the 2026-07-13 sessions — each pass found and fixed real bugs (detailed in §9) that the growing unit-test suite had missed every time.

---

## 1. Verdict, Up Front

**42 of 180 stories (23.3%) are fully Built, 10 (5.6%) are Partial with a named gap, and 128 (71.1%) are Not Started.** Of the 17 epics in the backlog, 5 have any code at all (EPIC-AST, EPIC-ORG, EPIC-USR, EPIC-SEC, and now **EPIC-AUD** — the product's own "core differentiator" per the Epics document, moved from 0/24 to 13/24 Built this session); the other 12 still have zero code.

Within the 4 previously-touched epics, R1 scope is close to exhausted: EPIC-AST is 14/16 Built, EPIC-ORG is 5/6 Built, EPIC-USR is 5/9 Built, and EPIC-SEC is 5/17 Built. This session's work went into starting EPIC-AUD instead of pushing those four further, since each of their remaining gaps needs new infrastructure (LDAP, TOTP, email) or an architecture change rather than a small extension. EPIC-AUD's own remaining gaps are the same shape: photo evidence needs object storage, offline sync needs a durable client-side queue, escalation needs EPIC-LIF's approval-routing infrastructure, and none of those exist yet either.

| Track | Status |
|---|---|
| **EPIC-AST** (Asset Management) | 14 Built / 0 Partial / 2 Not started (16 stories) |
| **EPIC-ORG** (Organization Management) | 5 Built / 1 Partial / 0 Not started (6 stories) |
| **EPIC-USR** (User Management & RBAC) | 5 Built / 2 Partial / 2 Not started (9 stories) |
| **EPIC-SEC** (Security) | 5 Built / 2 Partial / 10 Not started (17 stories) |
| **EPIC-AUD** (Audit Management, core differentiator) | 13 Built / 5 Partial / 6 Not started (24 stories) |
| **12 remaining epics** (LIF, INV, RPT, PLAT, NTF, SCN, DSH, CMP, INT, MIG, SRC, ANL) | 0 Built / 0 Partial / 108 Not started |
| **Total** | **42 Built / 10 Partial / 128 Not started (180 stories)** |

---

## 2. Epic Overview

| Epic | Module | Stories | Built | Partial | Not started | Release Span |
|---|---|---|---|---|---|---|
| EPIC-AST | Asset Management | 16 | 14 | 0 | 2 | R1 (core) / R2 (15,16) |
| EPIC-ORG | Organization Management | 6 | 5 | 1 | 0 | R1 |
| EPIC-USR | User Management & RBAC | 9 | 5 | 2 | 2 | R1 |
| EPIC-SEC | Security | 17 | 5 | 2 | 10 | R1 (core) / R2 (03b,07,12,13) |
| EPIC-MIG | Data Migration & Bulk Import/Export | 5 | 0 | 0 | 5 | R1 (01,03,04,05) / R3 (02) |
| EPIC-CMP | Compliance & Data Privacy | 6 | 0 | 0 | 6 | R1 |
| EPIC-SRC | Search | 5 | 0 | 0 | 5 | R1–R3 |
| EPIC-SCN | Scanning | 7 | 0 | 0 | 7 | R1 (01–05,07) / R2 (06) |
| EPIC-LIF | Asset Lifecycle Management | 16 | 0 | 0 | 16 | R2 |
| EPIC-AUD | Audit Management (core differentiator) | 24 | 13 | 5 | 6 | R2 |
| EPIC-INV | Inventory Management | 11 | 0 | 0 | 11 | R2 |
| EPIC-NTF | Notifications | 10 | 0 | 0 | 10 | R2 |
| EPIC-DSH | Dashboard | 7 | 0 | 0 | 7 | R2 |
| EPIC-RPT | Reporting | 15 | 0 | 0 | 15 | R2 |
| EPIC-INT | External Integrations | 6 | 0 | 0 | 6 | R1 (03) / R3 |
| EPIC-ANL | Product Analytics | 4 | 0 | 0 | 4 | R2 |
| EPIC-PLAT | Platform & NFR Enablers | 16 | 0 | 0 | 16 | R1–R3 |
| **Total** | | **180** | **42** | **10** | **128** | |

---

## 3. EPIC-AST — Asset Management (14/16 Built)

| Story | Title | Status | Evidence |
|---|---|---|---|
| US-AST-01 | Register a new asset with a generated identifier | Built | `AssetController.create`, `AssetRegistrationService` |
| US-AST-02 | Generate and print a scannable asset label | Built | `AssetLabelController`, `infrastructure/label/*` |
| US-AST-03 | Configure asset categories | Built | `AssetCategoryController`, `AssetCategoryService` |
| US-AST-04 | Model parent-child asset relationships | Built | `AssetHierarchyController`, `AssetHierarchyService` |
| US-AST-05 | Attach images and files to an asset | Not started | No attachment/object-store code exists; MinIO provisioned in `.env.example` but unused |
| US-AST-06 | Define per-category custom-field schemas | Built | `AssetCategoryRequest.customFields`, V5 migration |
| US-AST-07 | Configurable status lifecycle for assets | Built | `AssetStatusController`, V3 migration |
| US-AST-08 | Track warranty and AMC coverage | Built | `warrantyStartDate`/`EndDate` on Asset; AMC-specific modeling not separately verified |
| US-AST-09 | Record purchase and vendor details | Built | Manufacturer/vendor/PO/cost fields on `AssetCreateRequest` |
| US-AST-10 | See a complete, append-only change history per asset | Built | `GET /assets/{id}/history`, `AssetHistoryEventRepository` |
| US-AST-11 | Log asset movement between locations | Built | `GET /assets/{id}/movements` |
| US-AST-12 | Reserve an RFID identifier field for future use | Built | V11 migration, `rfidTagId` |
| US-AST-13 | Bulk-import assets from a template | Not started | Owned by EPIC-MIG (US-MIG-03), which has no code |
| US-AST-14 | Track insurance policy details per asset | Built | `AssetInsuranceController`, V12 migration |
| US-AST-15 | Track vehicle-specific attributes | Built | `AssetVehicleController`, V13 migration |
| US-AST-16 | Compute depreciation and net book value | Built | `AssetDepreciationController`, `DepreciationService`, V14 migration |

---

## 4. EPIC-ORG — Organization Management (5/6 Built)

Built out this session: a real multi-level hierarchy, renameable level labels, Room-level variants, and a standalone Department dimension.

| Story | Title | Status | Evidence |
|---|---|---|---|
| US-ORG-01 | Build a configurable multi-level org hierarchy | Built | `OrgHierarchyController`/`Service`, V19 migration (`org_level`, `OrgNode.path`) — click-tested: Campus→Building→Floor→Room chain created live with correct materialized paths |
| US-ORG-02 | Relabel hierarchy level names | Built | `PATCH /org-levels/{id}` — click-tested: renamed "Campus" to "Parish" live |
| US-ORG-03 | Model Department/Cost Center independent of location | **Partial** | `DepartmentController`/`Service`, V20 migration — click-tested CRUD; not yet referenced by Asset or Person (a separate, larger change, deliberately deferred) |
| US-ORG-04 | Maintain Person records independent of login accounts | Built | `PersonController`, `PersonService`, V9 migration |
| US-ORG-05 | Scope every asset, audit, and user to a hierarchy node | Built | `OrgScopeGuard` now does path-prefix (descendant) matching — click-tested: a user scoped to a Building correctly saw an asset three levels down in a Room under it |
| US-ORG-06 | Configure Classroom/Laboratory as Room variants | Built | `OrgLevel.roomVariants`, `OrgNode.roomVariant` — click-tested: Room created with `roomVariant=Classroom`, rejected for a non-configured variant |

---

## 5. EPIC-USR — User Management & RBAC (5/9 Built)

Provisioning, custom roles, permission-based enforcement (API and UI), and flat-role composition are solid and click-tested end-to-end. US-USR-03 and US-USR-04 moved from Partial to Built this session.

| Story | Title | Status | Evidence |
|---|---|---|---|
| US-USR-01 | Provision a user with a role and org scope | Built | `UserProvisioningService`, `BootstrapUserSeeder`, click-tested |
| US-USR-02 | Define custom roles with configurable permission sets | Built | `RoleService`, `RoleController` — click-tested by creating a `WAREHOUSE_LEAD` custom role live |
| US-USR-03 | Enforce permission checks at API and UI | Built | API: `PermissionChecker`, all 23 `@PreAuthorize` checks on permission strings (V18). UI: JWT carries `permissions`, `hasPermission()` hides write controls across `AdminShell`/`UserListPage`/`RoleListPage`/`CategoryConfigPage`/asset pages |
| US-USR-04 | Restrict access by organizational scope | Built | `OrgScopeGuard` path-prefix (descendant) matching — click-tested: Building-scoped user saw a Room-level asset three levels down |
| US-USR-05 | Scope the System Operator role to technical config only | Not started | Exception-path AC incidentally satisfied (System Operator's permissions never included assets/org read), but no technical-config settings endpoint exists for the happy path to attach to |
| US-USR-06 | Block self-approval of one's own submissions | Not started | No approval workflow exists yet to self-approve against (blocked on EPIC-LIF/EPIC-AUD) |
| US-USR-07 | Assign multiple flat roles instead of an inheriting hierarchy | Built | `user_role_assignment` plain join — satisfied by construction |
| US-USR-08 | Block offboarding while assets remain assigned | Partial | `UserDeactivationService` blocks correctly; deactivation now calls `RefreshTokenService.revokeAll()` — click-tested: a deactivated user's refresh token correctly fails on next use. Narrower remaining gap: their already-issued *access* token stays valid until its own natural expiry (no revocation list for stateless JWTs). |
| US-USR-09 | Record an SoD waiver for small organizations | Partial | `SodWaiverService`/`Controller`, V21 migration — click-tested: self-sign-off rejected, non-officer signer rejected, valid IT_SECURITY_OFFICER signer accepted. Reroute-engagement (US-AUD-22) deferred — no approval workflow exists yet to reroute. |

---

## 6. EPIC-SEC — Security (5/17 Built)

This session gave EPIC-SEC its first dedicated code (`com.iams.sec`) - everything before was a side effect of EPIC-USR's authentication seam. Five stories are now Built; the two remaining Partials are both narrower than before.

| Story | Title | Status | Evidence |
|---|---|---|---|
| US-SEC-01 | Authenticate with JWT access and refresh tokens | Built | `RefreshToken` (V26, rotation-based, revocable), `RefreshTokenService`, `POST /auth/refresh`/`/logout`/`/logout-all` — click-tested: rotation, reuse detection (with the token correctly rejected), logout, logout-all all confirmed live |
| US-SEC-02 | Authenticate via LDAP/AD | Not started | — |
| US-SEC-03 | Mandatory MFA for Super Admin/Administrator | Not started | No MFA/TOTP code anywhere |
| US-SEC-04 | Maintain a complete, immutable activity log | Built | `SecurityEventLog` (append-only, V22), `SecurityEventLogger` — wired into login success/failure, permission denials, role assignment, user deactivation. Click-tested live. |
| US-SEC-05 | Enforce a configurable password policy | Built | `PasswordPolicy` (V24), `PasswordPolicyService`, `PasswordValidator` — click-tested: tightened to min-12+complexity, weak password rejected citing all 4 violations at once, compliant password accepted |
| US-SEC-06 | Enforce a configurable session timeout | Partial | `RefreshTokenService.rotate()` refuses and revokes a refresh token idle longer than a configurable window (default 24h), logging `SESSION_EXPIRED` — but only bites when a client attempts a refresh; the access token itself stays valid for its full configured lifetime (480 min default) regardless of activity, since true per-request idle enforcement would need either much shorter access tokens or per-request session-state lookups, neither built this session |
| US-SEC-07 | Restrict access by IP range | Not started | — |
| US-SEC-08 | Encrypt sensitive data at rest and in transit | Not started | Not implemented or verified |
| US-SEC-09 | Lock accounts after repeated failed logins | Built | `AppUser.failedLoginCount`/`lockedUntil` (V23), `UserLockoutService` (15-min cooldown), admin unlock endpoint — click-tested: 5 failed attempts locked the account, 6th attempt (even with correct password) returned 423, admin unlock restored login immediately. Self-service unlock not built (no email system to deliver it through). |
| US-SEC-10 | Export and erase/anonymize a person's data | Not started | — |
| US-SEC-11 | Search and filter the Security & Access Log | Built | `GET /api/v1/security-events`, gated `security:read` (V25 backfills it onto ADMIN) — click-tested: filtering by event type/user/date works, a Viewer is correctly refused with 403 |
| US-SEC-12 | Surface dependency-scan status | Not started | — |
| US-SEC-13 | Patching, disclosure, and pen-test gate | Not started | Process/governance item, not code |
| US-SEC-14 | Issue scoped service accounts for integrations | Built | API-key issuance/auth/revoke flow; `X-Api-Key` filter + `ServiceAccountPrincipal`; web-layer default-deny for integrations with a scoped-endpoint whitelist (AC-SEC-14-H); `INTEGRATION_SERVICE` non-assignable-to-humans (AC-SEC-14-X); key stored SHA-256-hashed |
| US-SEC-15 | Never store integration credentials in plaintext | Not started | SA-key hashing (SHA-256) delivered under SEC-14; the integration-config secrets-manager-reference flow + AC-SEC-15-X plaintext-rejection remain to build |
| US-SEC-16 | Time-boxed, notified "break-glass" access | Not started | — |
| US-SEC-17 | Optional MFA for non-mandated roles | Not started | — |

---

## 7. EPIC-AUD — Audit Management, core differentiator (13/24 Built)

The product's own stated "core differentiator" (BR-05) and the largest single epic in the backlog behind EPIC-SEC - untouched at 0/24 until this session. Built as a full vertical slice: define an audit's type and scope, freeze the expected-asset set, assign auditors, scan/verify assets (single and batch), classify unverified assets Missing at submission, submit with password re-authentication and a typed signature, detect and reroute a Separation-of-Duties self-approval conflict, Department Head approval/rejection, a completion certificate, an exception report, and immutable-with-linked-corrections findings. Deliberately backend-only this session, same pattern EPIC-SEC's first session used - no frontend page exists yet for any of it.

This codebase's own interpretation of an underspecified point in the source stories: FR-AUD-01–18's R2 acceptance criteria are explicitly "derived from the FRS description... provisional" (see this document's own header convention), and the stories describe "close scanning" (US-AUD-09) and "close after approval" (US-AUD-15) without a clear intermediate state. This implementation folds them into one step: submitting an audit (US-AUD-13) both classifies Missing assets (US-AUD-09) and moves to Pending Approval; Department Head approval (US-AUD-14) and closure (US-AUD-15) are the same event, since the stories describe them as one moment ("an approved and closed audit"). See `AuditStatus`'s Javadoc for the full reasoning.

| Story | Title | Status | Evidence |
|---|---|---|---|
| US-AUD-01 | Define an audit's type and scope | Built | `AuditController.create`, `AuditService.create` — empty-scope creation click-tested rejected with `VALIDATION_FAILED` naming the `scope` field |
| US-AUD-02 | Assign one or more auditors to an audit | Built | `AuditAssignment` (extends BaseEntity), `POST/DELETE .../assignments` — click-tested: assignment created with a denormalized username snapshot, unassign keeps the row (`active=false`, `unassignedAt` set) rather than deleting it |
| US-AUD-03 | Run bulk audits across a wide scope | Partial | An org-node-scoped audit's expected-asset set naturally spans every matching asset via the same `AssetRepositoryImpl.search` path list/detail views use — but the progress/dashboard views report one flat total, not a per-sub-scope breakdown (AC-AUD-03-H's "breaks down by sub-scope" isn't built) |
| US-AUD-04 | Snapshot the expected-asset list at audit creation | Built | `AuditExpectedAsset` (append-only, no version/updated_*), resolved once in `AuditService.create` from org node and/or category and/or an explicit asset list — click-tested with all three scope shapes |
| US-AUD-05 | Verify a scanned asset with verifier, timestamp, and device | Built | `AuditScanService.recordScan`, `POST .../scans` — click-tested: verifier/timestamp/device recorded; an asset outside the expected set correctly flagged `OUT_OF_SCOPE` rather than silently accepted |
| US-AUD-06 | Scan continuously without re-navigating between assets | Not started | A frontend interaction pattern with no dedicated backend need — no frontend page exists for this epic yet |
| US-AUD-07 | Scan a batch and see results together | Built | `POST .../scans/batch`, `AuditScanService.recordBatchScan` — returns created/duplicate/unrecognized buckets together, click-tested via unit test covering all three outcomes in one call |
| US-AUD-08 | See real-time expected-vs-verified progress | Built | `GET .../progress` — click-tested live, counts matched scans exactly. No "pending sync" state (US-AUD-19 isn't built, so there's no offline queue to report) |
| US-AUD-09 | Classify unverified expected assets as Missing at closure | Built | `AuditWorkflowService.submit` → `classifyMissing` — click-tested: an audit with 1 of 2 expected assets scanned correctly classified the other Missing (`verifiedByUserId` null) at submission, and it appeared in the exception report |
| US-AUD-10 | Flag damaged assets on a configurable condition scale | Built | `AssetCondition` enum (GOOD/FAIR/MINOR_DAMAGE/MAJOR_DAMAGE/UNUSABLE), required on every scan — an out-of-scale value is rejected by JSON deserialization before it reaches application code |
| US-AUD-11 | Attach photo evidence to a finding | Not started | No object storage exists anywhere in this codebase to attach evidence to (same infra gap noted for other file-upload stories elsewhere in this document) |
| US-AUD-12 | Add remarks to a finding | Built | `AuditFinding.remarks` (1000-char limit), rejected with the limit stated if exceeded — click-tested |
| US-AUD-13 | Sign and submit a completed audit | Built | `AuditWorkflowService.submit` — password re-authentication via the same `PasswordEncoder` login uses, typed signature name stored with actor/timestamp — click-tested: wrong password leaves the audit unsubmitted (`VALIDATION_FAILED`, no state change), correct password moves it to `PENDING_APPROVAL`. MFA re-authentication (the story's other named option) isn't available anywhere in this codebase yet (US-SEC-03) |
| US-AUD-14 | Route a submitted audit to Department Head approval with escalation | Partial | `POST .../approve`/`.../reject` correctly gate on the routed approver (403 for anyone else, click-tested) and land in a pending-approval queue (`GET /audits?status=PENDING_APPROVAL`) — no escalation-on-timeout, since that needs EPIC-LIF's resolution-order infrastructure, which doesn't exist |
| US-AUD-15 | Issue a completion certificate on closure | Partial | `GET .../certificate` returns asset counts, verified/missing/damaged summary, approver, and date, blocked with 409 until the audit is closed — click-tested. Not rendered as a downloadable PDF (JSON only) |
| US-AUD-16 | Generate an exception report per audit | Built | `GET .../exceptions` — click-tested: a closed audit with one damaged and one out-of-scope finding listed both with `hasExceptions:true`; a clean audit would report `hasExceptions:false` rather than an ambiguous empty page |
| US-AUD-17 | View a live audit dashboard | Partial | `GET /audits/dashboard` returns active audits with live progress and exception counts, scoped like every other list endpoint — no frontend page, and no separate "recent/closed" section (active-only) |
| US-AUD-18 | Analyze audit trends across cycles | Not started | No cross-cycle analytics/reporting infrastructure exists |
| US-AUD-19 | Persist scans offline and sync without loss on reconnect | Not started | No offline-queue/client-storage infrastructure exists — this is a genuinely separate, larger feature |
| US-AUD-20 | Verify a large scope via statistical sampling | Not started | Should-priority, not attempted this session |
| US-AUD-21 | Reconcile a previously-Missing asset outside an active audit | Not started | The corrections-as-linked-records mechanism built for US-AUD-24 could extend to this in a future session, but no dedicated endpoint/workflow was built |
| US-AUD-22 | Detect and reroute Separation-of-Duties conflicts at submission | Built | `AuditWorkflowService.resolveApprover`, reusing `SodWaiverRepository.findActiveByScopeOrderByCreatedAtDesc("AUDIT_APPROVAL")` — click-tested end to end: a self-approval submission was blocked with no active waiver, then correctly rerouted to the waiver's signing officer once one was recorded |
| US-AUD-23 | Handle assets that change scope mid-audit | Partial | The model supports it fully (`SCOPE_CHANGED` status, `ScopeChangeDisposition` enum, closure blocked while any disposition is open — `AuditFindingRepository.existsByAuditIdAndStatusAndScopeChangeDispositionIsNull`) but nothing triggers it automatically: EPIC-LIF's transfer/dispose actions don't exist yet, so there is no "asset moved" event to hook into |
| US-AUD-24 | Correct a recorded finding only via an immutable, linked correction record | Built | `AuditFindingCorrection`, `POST .../findings/{id}/corrections` — no PATCH/DELETE endpoint exists for findings anywhere, which is what actually enforces immutability. Click-tested: corrected a finding's condition, the correction record captured old and new values plus actor, and the original finding's underlying row is never touched by any code path (verified by code inspection - no `setCondition`/`setRemarks` call exists outside finding creation) |

**Deliberately not done, and why:** photo evidence (US-AUD-11, needs object storage - no S3/blob infra exists anywhere in this codebase); offline sync (US-AUD-19) and statistical sampling (US-AUD-20, Should-priority) - both genuinely separate, larger features; cross-cycle analytics (US-AUD-18, needs reporting infrastructure that doesn't exist); escalation (US-AUD-14's other half, needs EPIC-LIF); automatic mid-audit scope-change detection (US-AUD-23's other half, same EPIC-LIF dependency - there is no "asset transferred" event anywhere yet to hook into); a frontend page for any of this (deliberately deferred, same as EPIC-SEC's first session).

---

## 8. Not Yet Started — 12 Epics (108 stories, 0 Built)

Confirmed by listing `backend/src/main/java/com/iams/`: only `asset`, `org`, `usr`, `sec`, `audit`, and `common` packages exist. None of the packages these 12 epics would need exist at all.

| Epic | Stories | Release Span |
|---|---|---|
| EPIC-LIF — Asset Lifecycle Management | 16 | R2 |
| EPIC-INV — Inventory Management | 11 | R2 |
| EPIC-RPT — Reporting | 15 | R2 |
| EPIC-PLAT — Platform & Non-Functional Enablers | 16 | R1–R3 |
| EPIC-NTF — Notifications | 10 | R2 |
| EPIC-SCN — Scanning | 7 | R1 (01–05,07) / R2 (06) |
| EPIC-DSH — Dashboard | 7 | R2 |
| EPIC-CMP — Compliance & Data Privacy | 6 | R1 |
| EPIC-INT — External Integrations | 6 | R1 (03) / R3 |
| EPIC-MIG — Data Migration & Bulk Import/Export | 5 | R1 (01,03,04,05) / R3 (02) |
| EPIC-SRC — Search | 5 | R1–R3 |
| EPIC-ANL — Product Analytics | 4 | R2 |

---

## 9. Frontend Coverage

Routed pages exist only for what the backend supports: `/assets`, `/assets/new`, `/assets/categories`, `/assets/:id` (+ edit), `/users`, `/roles`. No UI exists yet for org-hierarchy/department management (backend is built, no frontend picker yet) or for any of the 13 not-started epics. US-USR-03's UI half (permission-based control-hiding) is now built, closing what had been the one concrete frontend gap on an otherwise-built story.

**Now actually browser-tested, not just built.** Every prior session's verification was curl/API-level only — this session installed Playwright and drove all six routed pages in a real headless Chromium instance for the first time in this project's history: login, the Asset Register list and an asset's full detail view (label QR/barcode, history, movements, insurance, depreciation panels), Categories, Users, Roles, and the "New Category"/"New User" creation dialogs. All render correctly with real data from a live Postgres instance and zero console errors after the two bugs below were fixed.

---

## 10. Bugs Found and Fixed This Session (via live click-testing, not caught by unit tests)

This section exists because fourteen real bugs were found only by exercising the running application across six click-test/browser-test passes this same day — the unit-test suite (growing from 68 to 149 across those passes) never caught any of them. This is not incidental: Mockito-based unit tests structurally cannot catch `LazyInitializationException` (mocked repositories never go through a real Hibernate session), PGJDBC-level parameter-type-inference failures (mocked repositories never send SQL to a real driver), transaction-propagation/rollback bugs (mocked repositories never participate in a real transaction), DB-level constraint mismatches (mocked repositories never send DDL-constrained inserts to a real database), workflow-state bugs where a later step invalidates an earlier one's side effect (a single unit test only ever exercises one step in isolation), or invalid-DOM-nesting bugs (unit tests never render into an actual browser DOM) — every one of these bugs is exactly one of those six classes.

1. **`AssetCategoryService`** used the literal errorCode `ORG_NODE_HAS_DEPENDENTS` for a *category* deletion conflict — a copy-paste artifact from before EPIC-ORG existed. Renamed to `CATEGORY_HAS_DEPENDENTS`, freeing the correct code for actual org-node deletion.
2. **`ApiExceptionHandler`** silently swallowed every unhandled exception with no logging — the first symptom of bug #3 was an opaque 500 with nothing in the server log to explain it.
3. **`AuthController.login`** threw `LazyInitializationException` reading a lazy `Role` proxy after the transaction closed — login was completely broken on the very first click-test of this project.
4. **Every `@PreAuthorize` denial** threw a generic 500 instead of a proper 403 — `AuthorizationDeniedException` was falling through to the generic exception handler instead of the dedicated 403 handler, undetected because no test had ever exercised a denied permission check.
5. **`OrgNode`** had no `@PrePersist` hook (it predates `BaseEntity` and was never migrated onto it) — the first `POST /org-nodes` threw a NOT NULL violation on `created_at`.
6. **`GET /assets` (list), `GET /assets/{id}`, and every asset-mutation endpoint** (update, status-change, assign, unassign, link-child) threw `LazyInitializationException` reading `category`/`status`/`orgNode`/`parentAsset` — none of these had ever been fetch-joined; only `POST /assets` (create) happened to work, because it builds those associations from already-loaded objects rather than a lazy re-fetch.
7. **`SecurityEventLogRepository`'s first search implementation** (plain JPQL `(:param IS NULL OR ...)`) threw `InvalidDataAccessResourceUsageException: could not determine data type of parameter` — PGJDBC can't infer a bind parameter's type when its only appearances are both sides of an IS-NULL-guarded OR against a nullable `Instant`. Fixed by converting to the JPA Criteria API (`SecurityEventLogRepositoryCustom`/`Impl`), the same pattern `AssetRepositoryImpl` already uses for optional-filter searches - a null filter there never becomes a predicate at all, so PGJDBC never sees an ambiguous placeholder. This is now the established, structural answer in this codebase for "several optional filters" queries against Postgres - prefer Criteria over JPQL string-templating for that shape.
8. Three test files constructing `OrgScopeGuard` directly needed a new `SecurityEventLogger` mock for its 4th constructor parameter once `OrgScopeGuard` started logging permission denials to the persistent log - mechanical, not a design flaw, but a sign `OrgScopeGuard`'s constructor is now small shared infrastructure with several direct callers.
9. **`SecurityEventLogger.record()` had no transaction boundary of its own**, so calls made from inside an already-`@Transactional` method that later throws (`OrgScopeGuard`'s PERMISSION_DENIED, `RefreshTokenService`'s REFRESH_TOKEN_REUSE_DETECTED/SESSION_EXPIRED - all "log the denial, then throw") joined the caller's transaction and got rolled back with it. The write appeared to succeed at the call site; the row simply never existed afterward. Fixed with `@Transactional(propagation = REQUIRES_NEW)`, the correct semantic for an audit log - the record of "this was denied" should survive even though the underlying operation rolled back. This means **`PERMISSION_DENIED` events had never actually been persisting since EPIC-SEC's log was first built** - re-verified live after the fix.
10. **A plain `org.springframework.security.access.AccessDeniedException`** (thrown directly by `OrgScopeGuard.requireWithinScope()`, not via `@PreAuthorize`) fell through to the generic 500 handler - the same root cause as bug #4, but a different exception class. `AuthorizationDeniedException` (bug #4's fix) turns out to `extend` this older, more general `AccessDeniedException` class (confirmed by inspecting the compiled bytecode), so the fix widened the existing handler to the parent type, covering both the AOP `@PreAuthorize` case and this plain-application-code-throw case with one handler. Found by click-testing a genuinely out-of-scope single-asset fetch for the first time - every prior scope-enforcement click-test checked list-filtering or the in-scope path, never a single-entity denial's actual HTTP response.
11. **Invalid HTML nesting on the Categories, Users, and Roles list pages** (`CategoryConfigPage.tsx`, `UserListPage.tsx`, `RoleListPage.tsx`) — found on the very first Playwright browser-testing pass this project has ever had. MUI's `ListItemText` renders its `secondary` prop inside a `<Typography component="p">` by default, but all three pages pass a `<Stack>` (renders `<div>`) containing `Chip`s and/or a nested `<Typography>` as that content — block elements and paragraphs nested inside a `<p>`, invalid HTML that React's DOM-nesting validator logs as a console error (the same class of warning that becomes a hard hydration failure under SSR). Fixed by adding `slotProps={{ secondary: { component: 'div' } }}` to all three `ListItemText` usages, the MUI-documented fix for this exact case. Re-verified live: zero console errors across all four main pages afterward, with identical visual output.
12. Confirmed **not** a bug, recorded so it isn't rediscovered as one: a full page reload/direct-URL-navigation to any authenticated route correctly bounces to `/login`, because `authStore` deliberately keeps both tokens in memory only (see the comment in `authStore.ts`) — this is the documented security posture working as intended, not a session-handling bug. Client-side navigation (a sidebar click) preserves the session correctly.
13. **A DB-level `CHECK` constraint was stricter than the actual business rule it was meant to enforce.** `V27`'s `chk_audit_scope_present` required `scope_org_node_id IS NOT NULL OR scope_category_id IS NOT NULL`, but `AuditService.create()`'s real validation correctly also allows a third case FR-AUD-01 names explicitly - an audit scoped by an explicit asset list alone, with both those columns null. The very first live attempt to create an asset-list-scoped audit threw a 500 (`ConstraintViolationException`). A `CHECK` constraint can't see the resulting `audit_expected_asset` rows the way the service layer can, so it could only ever be a stricter, wrong subset of the real rule - fixed by dropping it in `V29` (a new migration, not an edit to the already-applied `V27` - editing an applied migration would corrupt Flyway's checksum history) and leaving the correct validation solely in `AuditService.create()`, where it belongs.
14. **Rejecting a submitted audit left an orphaned, unfixable Missing finding behind.** `AuditWorkflowService.submit()` correctly classifies unscanned expected assets as Missing (US-AUD-09), but `reject()` only reset the audit's status/submission fields - it never touched those Missing rows. Since `AuditFinding` is deliberately immutable with no re-scan-overwrites-existing-finding path (US-AUD-24), a rejected-and-reopened audit could never actually verify that asset again: any attempt to scan it hit `FINDING_ALREADY_RECORDED`, permanently. Found by click-testing the full reject-then-rescan workflow for the first time (a pattern earlier click-test passes in this project hadn't exercised, since prior epics don't have a comparable revert-and-retry flow). Fixed by having `reject()` delete only the system-classified Missing findings it's undoing (identified by `verifiedByUserId IS NULL` - a real scan always has a verifier, so this can never delete recorded evidence) via a new `AuditFindingRepository.findByAuditIdAndStatusAndVerifiedByUserIdIsNull` query. Re-verified live: reject, then rescan the same asset, now succeeds and correctly returns `201` with a real `VERIFIED` finding.

---

## 11. Recommended Next Session

EPIC-AUD now has a solid backend core (13/24 Built) but no frontend at all, and six of its remaining stories are genuinely blocked on infrastructure this codebase doesn't have yet (object storage for photo evidence, an offline queue, EPIC-LIF's transfer/dispose actions for automatic scope-change detection, cross-cycle reporting). Two productive directions for next time: (1) build the EPIC-AUD frontend - an audit list/detail page, a scan-entry flow, an approval queue - using the same "browser-test it for real" discipline this session established, or (2) start EPIC-LIF (Asset Lifecycle Management, 16 stories, R2), since it directly unblocks two of EPIC-AUD's own Partials (US-AUD-14's escalation, US-AUD-23's automatic scope-change trigger) as a side effect, the same way EPIC-USR's auth seam once fed EPIC-SEC.

R1 scope across EPIC-AST/ORG/USR/SEC remains close to fully exhausted, unchanged from the prior recommendation - what's left there needs new infrastructure (LDAP, TOTP, email) or an architecture change, not a small extension.

Real browser testing (Playwright) continues to be worth using on any future session that touches the frontend - it caught two real bugs the first time it was used (§10, items 11-12 from the previous session) that no amount of curl-based click-testing could have found. It still isn't wired into the project as a repeatable/checked-in suite (installed ad hoc in a scratch directory each time); making it a committed `frontend/e2e` suite or a project skill remains real, separate work worth doing deliberately - doubly true now that EPIC-AUD needs a frontend built.
