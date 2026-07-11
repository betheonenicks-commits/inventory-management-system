# Functional Requirements Specification (FRS)
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-FRS-2.0 | **Version:** 2.0 (Consolidated Development Baseline) | **Status:** For Ratification | **Date:** 2026-07-10

> **Supersedes** FRS v1.2 and all draft references to "IAMS-FRS-3.0/4.0". Resolves the five requirement-ID collisions and adopts the four modules (MIG, INT, CMP, ANL) previously specified only in downstream artifacts. **Appendix B is the authoritative old→new ID map**; the Data Dictionary v1.1 and API Spec v1.1 have been updated to these IDs. Traceability: every FR maps to a BR in BRD 2.0 §7 and to a release in §5.

## 1. Conventions
Requirement IDs: `FR-<MODULE>-<NN>`. Priority: MoSCoW. Sector variation is handled through configuration, not separate requirements. Modules: AST, INV, ORG, LIF, AUD, SCN, RPT, DSH, USR, NTF, SRC, SEC, MIG, INT, CMP, ANL.

## 2. Functional Requirements by Module

### 2.1 Asset Management (AST) — BR-01, BR-14
Unchanged from v1.2: **FR-AST-01** registration with system-generated unique asset number (format `AST-YYYY-NNNNNN`; the asset number is the barcode/QR payload) [Must]. **FR-AST-02** barcode/QR generation, renderable and printable as a label [Must]. **FR-AST-03** configurable categories and groups [Must]. **FR-AST-04** parent-child asset relationships [Should] — *clarified: transferring or disposing a parent prompts explicit disposition of children (move-with-parent, detach, or block); children appear as distinct expected items in audits.* **FR-AST-05** images and file attachments per asset [Must]. **FR-AST-06** per-category custom fields (text, number, date, single/multi-select) without code changes [Must]. **FR-AST-07** configurable status list (In Use, In Storage, Under Repair, Missing, Retired, Disposed, Void) [Must]. **FR-AST-08** warranty and AMC period tracking [Must]. **FR-AST-09** manufacturer, vendor, purchase date/cost, PO reference [Must]. **FR-AST-10** complete append-only change history [Must]. **FR-AST-11** movement history log [Must]. **FR-AST-12** RFID architecture extension point, no Phase-1 hardware [Should]. **FR-AST-13** bulk asset import from defined CSV/Excel template with pre-commit validation report and post-import reconciliation (implemented via module MIG) [Must].

New in 2.0 (renumbered from draft artifacts — see Appendix B):
**FR-AST-14** — Track insurance policy details per asset (insurer, policy number, coverage amount, expiry) with expiry reporting. [Should]
**FR-AST-15** — Support Vehicle asset subtype attributes (VIN, registration number, odometer, registration/insurance expiry) via dedicated fields. [Should]
**FR-AST-16** — Store depreciation parameters — method (Straight-Line at minimum; Written-Down-Value optional), useful life in months, salvage value — as per-category defaults with per-asset override, and compute a depreciation schedule and net book value from purchase cost, in-service date, and a monthly (full-month) convention against the organization's configured fiscal year. [Should — required by FR-RPT-09]

### 2.2 Inventory Management (INV) — BR-06
Unchanged: **FR-INV-01** quantity tracking separate from assets [Must]. **FR-INV-02** Stock In/Out with reason codes and responsible user [Must]. **FR-INV-03** multiple warehouses with shelf/bin sub-locations [Must]. **FR-INV-04** configurable reorder levels with below-threshold flagging [Must]. **FR-INV-05** manual adjustments with mandatory reason and approver [Must]. **FR-INV-06** configurable costing method (weighted average at minimum) [Should]. **FR-INV-07** vendor records linked to purchase history [Must]. **FR-INV-08** full vendor CRUD independent of items, including deactivation; inter-warehouse transfers recorded as a linked transaction pair, executed atomically [Must].

New: **FR-INV-09** — Lot/batch registration with optional expiry date and expiring-stock visibility (configurable lookahead). [Should]
**FR-INV-10** — Multi-currency purchase capture: original currency/amount plus FX rate and as-of date captured at entry; the stored reporting-currency amount computed at entry time is what all aggregates use. [Should]
**FR-INV-11** — Stock units of measure: each inventory item carries a unit of measure (each, box, kg, litre, …) displayed on all transactions and reports; no automatic UoM conversion at initial release. [Should]

### 2.3 Organization Management (ORG) — BR-02
Unchanged v1.2 set: **FR-ORG-01** configurable multi-level hierarchy [Must]; **FR-ORG-02** relabelable level names [Must]; **FR-ORG-03** Department/Cost Center independent of physical location [Must]; **FR-ORG-04** Employee/Volunteer records — *clarified: a Person record is distinct from a login account; a Person may hold assets without ever logging in (DD v1.1 `person` entity)* [Must]; **FR-ORG-05** scoping of any asset, audit, or user to hierarchy nodes [Must]; **FR-ORG-06** Classroom/Laboratory as configurable Room variants [Should].

### 2.4 Asset Lifecycle Management (LIF) — BR-04
Unchanged: **FR-LIF-01**–**FR-LIF-10** (purchase request/approval; PO creation & linkage; receiving reconciliation; assignment; transfer with configured approval; repair logging; preventive maintenance; corrective maintenance; retirement/disposal/donation with reason+approver+date; immutable lifecycle history). **FR-LIF-11** rejection with mandatory reason, requester notification, return to draft [Must]. **FR-LIF-12** restore of Retired/Disposed within configurable window, Admin+, new immutable event [Should]. **FR-LIF-13** approval-trigger rules and escalation to alternate approver after configurable inaction days [Should]. **FR-LIF-14** block erasure while assets remain assigned (see FR-SEC-10) [Must].

New: **FR-LIF-15** — Approver delegation: an approver (or an Administrator on their behalf) may delegate their approval authority to a named alternate for a defined period; FR-LIF-13/FR-AUD-13 escalation selects, in order: the active delegate, the Department Head's line manager (department hierarchy), then Administrator. All delegations are logged. [Must]
**FR-LIF-16** — PO edge handling: partial receipt (line-level received quantities), PO cancellation/amendment before receipt with reason, and return-to-vendor of received-but-rejected items, each producing immutable lifecycle records. [Should]

### 2.5 Audit Management (AUD) — BR-05, Core Differentiator
Unchanged: **FR-AUD-01**–**FR-AUD-18** as v1.2 (audit types and scoping; bulk audits; expected-asset snapshot at creation; scan verification with verifier/timestamp/device; continuous scan mode; batch scanning; real-time expected-vs-verified; Missing classification at closure; damaged flagging with structured condition scale — Good/Fair/Minor Damage/Major Damage/Unusable, configurable; photo evidence; remarks; digital signature at submission; Department Head approval routing with escalation; completion certificate; exception report; audit dashboard; cross-cycle analytics; immutability with corrections-as-linked-records).

**FR-AUD-12 clarification (signature type):** the digital signature is an **electronic signature**: the auditor's typed full name plus a mandatory credential re-authentication (password or MFA) at submission; the system records name, user ID, timestamp, and authentication event as the signature record. Cryptographic signatures are a future option.

**FR-AUD-19 — priority raised to Must Have** (resolves the Should-Have vs NFR-AVAIL-03 guarantee mismatch): offline scan queue persisting scans (and captured photo evidence) durably on the device (persistent browser storage), surviving app/browser restart, with visible queue depth and per-item sync status, synchronizing without loss or double-count (idempotency-keyed) when connectivity resumes. Covers short, localized gaps per SRS §8 Assumption 2. [Must]

New (formalized from draft artifacts): **FR-AUD-20** statistical sampling mode for large scopes with sample-size preview; null sampling = 100% verification [Should]. **FR-AUD-21** reconciliation of a previously-Missing asset outside an active audit, as a new record linked to the original finding, updating analytics per BRD §1.3.1 [Must]. **FR-AUD-22** SoD conflict detection at submission: if the nominal approver is the submitter, reroute to an alternate under an active waiver, or block submission with guidance; never silently self-approve or strand the audit [Must]. **FR-AUD-23** scope-change handling: assets transferred/disposed mid-audit are flagged "Scope Changed During Audit"; closure is blocked until each is dispositioned (Confirm Verified at New Location / Exclude from Scope / Accept as Exception) [Must].

### 2.6 Scanning (SCN) — BR-03
Unchanged: **FR-SCN-01** USB keyboard-wedge [Must]; **FR-SCN-02** Bluetooth HID [Must]; **FR-SCN-03** camera scanning (Android/iPhone/webcam) in browser [Must]; **FR-SCN-04** duplicate-scan detection per session/audit [Must]; **FR-SCN-05** scan-to-resolution ≤1s p95 [Must]; **FR-SCN-06** RFID-ready abstraction layer [Should].
New: **FR-SCN-07** — Published symbology and label configuration: Code 128 barcodes and QR (error-correction level M minimum); label sizes configurable against common thermal label stocks (default 50×25mm and 100×50mm); label output as PNG/SVG/PDF sized to stock, printable via standard OS print drivers (no proprietary printer SDK dependency at initial release). [Must]

### 2.7 Reporting (RPT) — BR-10
**FR-RPT-01**–**FR-RPT-15** unchanged from v1.2 (asset register; dept/room/building inventory; employee asset list; missing/lost/damaged; warranty & AMC expiry with lookahead; purchase/vendor; asset movement; audit compliance & summary; depreciation via FR-AST-16 parameters; maintenance history; batch label printing; PDF/Excel/CSV export + print; scheduled reports; security & access log report; ad hoc saved report builder [Could]). *FR-RPT-05 extended to include insurance expiry (FR-AST-14).*

### 2.8 Dashboard (DSH) — BR-08
**FR-DSH-01**–**FR-DSH-07** unchanged (totals and breakdowns; audit completion %; expirations & maintenance due; low stock; activity feed & audit calendar; configurable KPIs; role/scope filtering). *Clarification: dashboard data may be cached up to 5 minutes; the audit-progress widget must be near-real-time (≤30s staleness).*

### 2.9 User Management & RBAC (USR) — BR-11, BR-21
**FR-USR-01** default roles: Super Administrator, Administrator, **System Operator**, Inventory Manager, Auditor, Read-only Auditor, Department Head, Employee/Volunteer, Viewer; plus two system-provided custom roles shipped predefined — **IT Security Officer**, **Compliance Officer** — built on FR-USR-02; plus a non-assignable-to-humans **Integration Service** role (FR-SEC-14) [Must]. **FR-USR-02** custom roles with configurable permission sets [Should]. **FR-USR-03** permission checks at API and UI for every protected action [Must]. **FR-USR-04** org-scope restriction of access [Must]. **FR-USR-05** System Operator scoped to technical configuration only, no business data beyond operational need [Must]. **FR-USR-06** SoD: no self-approval of own submissions [Must]. **FR-USR-07** flat, non-inheriting role model; one primary role plus scope restrictions [Should].
New: **FR-USR-08** — Offboarding: deactivation of a user/person with assets still assigned is blocked with the blocking-asset list and resolution actions (reassign or return-to-inventory); once clear, deactivation proceeds and is logged. (Was draft "FR-USR-05".) [Must]
**FR-USR-09** — SoD waivers: recordable waiver scoped to org node + action types, requiring non-self-asserted IT Security Officer sign-off, with effective period, revocation, and full logging; waivers enable the FR-AUD-22 reroute path. (Was draft "FR-USR-07".) [Must]

### 2.10 Notifications (NTF) — BR-09
**FR-NTF-01** email notifications for the catalog events [Must]. **FR-NTF-02** optional SMS via configured gateway [Could]. **FR-NTF-03** in-app notifications with read/unread [Must]. **FR-NTF-04** assignment/transfer notifications to affected holder, source and destination approvers [Must]. **FR-NTF-05** per-user, per-event-type channel preferences; Administrator-mandated types are locked and visibly non-editable [Must].
New: **FR-NTF-06** — Trigger catalog (parameterized, all values configurable):

| Event | Default trigger | Default recipients | Repeat/dedupe |
|---|---|---|---|
| Upcoming audit | 7 days and 1 day before scheduled start | Assigned auditor; Dept Head of scope | Once per threshold |
| Overdue audit | Day after scheduled end while not closed | Auditor; Dept Head; Administrator | Every 3 days until closed |
| Warranty / AMC / insurance expiry | 60 and 14 days before expiry | Inventory Manager; asset's Dept Head | Once per threshold |
| Maintenance due | 7 days before scheduled date | Inventory Manager | Once, then on due date |
| Low stock | On crossing below reorder level | Inventory Manager | Once per crossing; daily digest while below |
| Pending approval | On submission | Routed approver (incl. delegate per FR-LIF-15) | Reminder every 2 days; escalation per FR-LIF-13 |
| Security alert (lockout, break-glass, permission change) | Immediate | Super Admin; IT Security Officer | Immediate, mandatory (locked) |

**FR-NTF-07** — Recipient resolution is computed by role × org-scope at send time (e.g., "Dept Head of scope" resolves through the department hierarchy, honoring delegation). [Must]
**FR-NTF-08** — Delivery tracking and retry: email/SMS deliveries are queued through the transactional outbox, retried with exponential backoff (default 3 attempts), and logged with status; failed approval-class notifications raise an in-app alert to an Administrator. In-app delivery is the always-available fallback channel. [Must]
**FR-NTF-09** — Versioned notification templates per event type and channel, with variable substitution; English at initial release, externalized per NFR-UX-05. [Should]
**FR-NTF-10** — Notification deep links: links to approvals/audits authenticate via the normal login (honoring SSO/MFA) and return the user to the linked resource post-login; expired sessions never dead-end the approval. [Must]

### 2.11 Search (SRC) — BR-03
**FR-SRC-01**–**FR-SRC-05** unchanged (global search; direct code lookup; advanced combinable filters; saved searches [Should]; reserved RFID field [Could]).

### 2.12 Security (SEC) — BR-07, BR-13
**FR-SEC-01**–**FR-SEC-09** unchanged (JWT auth; optional LDAP/AD; **FR-SEC-03a** mandatory MFA for Super Admin/Admin [Must] / **FR-SEC-03b** optional MFA for others [Could]; complete activity log; configurable password policy; configurable session timeout; optional IP restrictions [Could] — *with a documented mobile-auditor exemption strategy*; encryption of sensitive data at rest and in transit; lockout after N failed attempts (default 5) with logged unlock). **FR-SEC-10** data-subject export and erasure/anonymization, subject to retention holds and FR-LIF-14 [Must]. **FR-SEC-11** Security & Access Log searchable/filterable by an Administrator [Must].
New: **FR-SEC-12** dependency-scan status visibility (latest CI scan summary) to Super Admin/IT Security Officer [Should]. **FR-SEC-13** documented vulnerability patching and disclosure policy for on-premises customers; a third-party penetration test is a go-live gate for the first production deployment [Must — process requirement]. **FR-SEC-14** scoped service accounts for integrations (e.g., read-only accounting export), never blanket admin credentials, using the Integration Service role [Must]. **FR-SEC-15** credentials and integration secrets are never stored in plaintext: secrets-manager/encrypted references only, per SRS NFR-SEC-09 (was mis-cited as "FR-SEC-11" in DD 1.0) [Must]. **FR-SEC-16** break-glass: a Super Administrator may invoke time-boxed (default 4h) emergency elevated access with a mandatory reason; invocation immediately notifies the IT Security Officer and one other Administrator, every action during the window is flagged in the activity log, and the event appears in the security report until reviewed [Must].

### 2.13 Data Migration & Bulk Import/Export (MIG) — BR-12 *(adopted module)*
**FR-MIG-01** downloadable CSV/Excel import templates for Asset, Person/Employee, Vendor (and Inventory Item) [Must]. **FR-MIG-02** bulk export in a re-import-compatible format [Should — R3]. **FR-MIG-03** dry-run validation with per-row error report, explicit commit of valid rows, and reconciliation report (created/updated/rejected + generated identifiers); commit permitted with outstanding error rows (rejected rows never silently dropped) [Must]. **FR-MIG-04** import run history (who, when, counts, outcome) [Must]. **FR-MIG-05** migration cutover runbook: rehearsal import on a staging copy, agreed acceptance thresholds, and a documented rollback (restore-from-backup) step are required before production cutover [Must — process requirement].

### 2.14 External Integrations (INT) — BR-16 *(adopted module)*
**FR-INT-01** read-only accounting/ERP export of depreciation and valuation data (CSV/JSON, stable documented column schema); no write path back into IAMS [Must — R3]. **FR-INT-02** HR/SIS roster sync (pull), manual or scheduled [Should — R3]. **FR-INT-03** identity-provider integrations (LDAP/AD, SAML2/OIDC SSO) — configured under SEC, available R1 [Must]. **FR-INT-04** outbound webhooks to Administrator-registered, allow-listed URLs with delivery log and retries [Should — R3]. **FR-INT-05** integration registry: every integration disabled by default; enabling an outbound data flow requires recorded Compliance Officer review (BRD §6.5); enable/disable events logged [Must]. **FR-INT-06** webhook payload signing: HMAC-SHA256 over raw body with per-webhook secret; constant-time verification documented for receivers (was mis-cited as "FR-SEC-10" in API 1.0) [Must — with FR-INT-04].

### 2.15 Compliance & Data Privacy (CMP) — BR-17, BR-18 *(adopted module)*
**FR-CMP-01** per-entity-type retention policies (period + action at expiry: delete/anonymize/hold-eligible) honoring BRD §5.4 floors [Must]. **FR-CMP-02** anonymization workflow for departed persons: flagged at eligibility, Compliance Officer approval, stable pseudonymous reference preserved in historical records; blocked by legal hold or FR-LIF-14 [Must]. **FR-CMP-03** configurable privacy-notice text per personal-data field [Should]. **FR-CMP-04** accessibility-audit status record (latest WCAG 2.1 AA audit, informational) [Should]. **FR-CMP-05** data-residency confirmation view: all stores on-premises; flags any enabled outbound integration [Must]. **FR-CMP-06** legal hold on an asset or audit record blocking retention purge and erasure until lifted, with reason and full logging [Must].

### 2.16 Product Analytics (ANL) — BR-20 *(adopted module)*
**FR-ANL-01** server-side capture of feature-usage events [Should]. **FR-ANL-02** analytics data never transmitted outside the deployment; deliberately no client-side event-submission API [Must]. **FR-ANL-03** usage report (adoption by role, module frequency) for Administrators [Should]. **FR-ANL-04** in-app feedback submission (category + free text) routed to a configured recipient [Should].

## 3. Traceability
Every FR traces to a BR (BRD 2.0 §7); forward traceability to endpoints is maintained in API Spec 1.1 (FR-ID column) and to schema in DD 1.1. The RTM workbook is the living matrix.

## 4. Acceptance Criteria
Given/When/Then acceptance criteria for **all R1 Must Have requirements** are delivered in **IAMS-AC-1.0** (companion document) — one happy path and one exception path per requirement, per the v1.2 §4 policy, now fulfilled for R1. R2 Must Haves receive stubs before their first sprint; Should/Could defer to backlog-level criteria.

## 5. Release Mapping Appendix (requirement level)
**R1:** FR-AST-01–14; FR-ORG-01–06; FR-USR-01–09; FR-SEC-01, 02, 03a, 04–06, 08–11, 14–16; FR-MIG-01, 03, 04, 05; FR-CMP-01–06; FR-SRC-01–03; FR-SCN-01–05, 07; FR-NTF-10 (auth deep-link behavior ships with auth); FR-INT-03.
**R2:** FR-AST-15, 16; FR-INV-01–11; FR-LIF-01–16; FR-AUD-01–23; FR-SCN-06; FR-RPT-01–15; FR-DSH-01–07; FR-NTF-01–09; FR-SRC-04; FR-SEC-03b, 07, 12, 13; FR-ANL-01–04.
**R3:** FR-INT-01, 02, 04, 05, 06; FR-MIG-02; FR-SRC-05.

## Appendix B — Requirement-ID Reconciliation Map (authoritative)
| Draft ID (DD 1.0 / API 1.0 / PUC 1.0) | Meaning | **FRS 2.0 ID** |
|---|---|---|
| FR-AST-13 (draft: insurance) | Insurance tracking | **FR-AST-14** |
| FR-AST-14 (draft: vehicle) | Vehicle attributes | **FR-AST-15** |
| FR-AST-15 (draft: depreciation) | Depreciation parameters/calc | **FR-AST-16** |
| FR-USR-05 (draft: offboarding) | Offboarding block | **FR-USR-08** |
| FR-USR-07 (draft: SoD waivers) | SoD waivers | **FR-USR-09** |
| FR-SEC-10 (draft: webhook secret) | Webhook signing | **FR-INT-06** |
| FR-SEC-11 (draft: no plaintext creds) | Credential storage | **FR-SEC-15** |
| Unchanged meanings | FR-AST-13 = bulk import; FR-USR-05 = System Operator scoping; FR-USR-07 = flat role model; FR-SEC-10 = data-subject erasure; FR-SEC-11 = Security & Access Log | as in v1.2 |
| FR-AUD-20/21/22/23, FR-SCN-07, FR-NTF-05, FR-INV-09/10, FR-SEC-12/13/14, FR-CMP-01–06, FR-INT-01–05, FR-MIG-01–04, FR-ANL-01–04 | Previously downstream-only | **Adopted, same numbers** |
