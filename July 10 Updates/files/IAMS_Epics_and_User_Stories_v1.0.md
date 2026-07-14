# Epics and User Stories
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-EUS-1.1 | **Status:** Draft for Review | **Date:** 2026-07-12
**Derived from:** IAMS-BRD-2.0, IAMS-FRS-2.0, IAMS-SRS-2.0, IAMS-AC-1.0, IAMS-DD-1.1 Amendment, IAMS-PUC-1.1 (see `July 10 Updates/files/`). Where this document conflicts with any of those, they govern — this is a delivery-planning artifact, not a new requirements baseline.

> **v1.1 changelog (2026-07-12, adversarial review against the FRS/API Spec/Data Dictionary/RTM and the `UX design/IAMS Design System` UI kit):** despite `IAMS_RTM_Report_v1.1.md` §1 claiming "178 stories, verified 1:1 FR traceability" and 0 open issues, two FRs had no story at all: **FR-AUD-18** (audit/finding immutability — independently cited by the API Spec's corrections endpoint, the DD's `version` column, and the RTM itself) and **FR-SEC-03b** (optional MFA for non-mandated roles — cited in this document's own EPIC-SEC release-span note but never written up). Added **US-AUD-24** and **US-SEC-17** to close both. Also corrected 7 story statements that used a role name not on FR-USR-01's list, contradicting this document's own §1 convention that role names must match FR-USR-01 exactly: 5 stories (US-DSH-06, US-RPT-01/09/12, US-INT-01) said "Board Viewer" (not a real role — the closest is "Viewer"; independently confirmed absent from the design system's closed `users[]` role vocabulary), and 2 stories (US-NTF-04/05) said "Employee" instead of the actual combined role "Employee/Volunteer". Story/epic counts below are updated accordingly (178→180); RTM and the `.xlsx` tracker should be corrected to match in a follow-up pass.

> **Coverage:** All 16 FRS modules, all three releases (R1/R2/R3), plus a 17th cross-cutting **Platform & Non-Functional Enablers** epic covering SRS architecture/NFR items that have no FR counterpart (object storage, concurrency, observability, rate limiting, deployment, accessibility). 163 functional requirements are represented 1:1 as user stories (a handful of FRs that are pure sub-clauses of a parent FR are folded into that parent's acceptance criteria and noted inline), plus 16 platform stories.

## 1. Conventions

- **Epic ID:** `EPIC-<MODULE>` (module codes per FRS §1: AST, INV, ORG, LIF, AUD, SCN, RPT, DSH, USR, NTF, SRC, SEC, MIG, INT, CMP, ANL, plus PLAT).
- **User Story ID:** `US-<MODULE>-<NN>`, numbered to match the FR it implements (`US-AST-01` implements `FR-AST-01`) so traceability to the FRS and to `IAMS-AC-1.0` is immediate. Platform stories use `US-PLAT-<NN>` and cite SRS section/NFR IDs instead of an FR.
- **Story format:** *As a `<role>`, I want `<capability>`, so that `<benefit>`.*
- **Priority:** MoSCoW, carried from the FRS where the FRS marks it explicitly on that requirement. Where the FRS states a priority once for a *block* of unchanged, carried-forward requirements (e.g., "FR-LIF-01–FR-LIF-10 unchanged") without breaking it out per item, this document defaults those items to **Must** — they are baseline v1.2 functionality the release plan already treats as core — and flags the assumption at the epic's opening note. Items the FRS tags individually (`[Must]`/`[Should]`/`[Could]`) use that exact tag.
- **Release:** R1 / R2 / R3 per FRS §5 Release Mapping Appendix, verbatim.
- **Acceptance criteria:** Given/When/Then, one happy-path and one exception-path per story wherever possible. For R1 stories with a published scenario in `IAMS-AC-1.0`, that scenario is paraphrased here (see the `AC-xxx` citation) — treat `IAMS-AC-1.0` as the source of truth for exact wording and API error codes. For R2/R3 stories, acceptance criteria are **derived from the FRS description by this document** and are provisional pending the AC-2.0 companion the FRS §4 promises before each requirement's first sprint — they are a planning aid, not a ratified test spec.
- **Persona references** (Priya/Marcus/Elena/Devon/Grace/Fr. Thomas–Dr. Osei/Sam/Board-Viewer/Officer Reyes) are the nine personas in `IAMS-PUC-1.1` §1; role names in story statements match FRS FR-USR-01 role names.

## 2. Epic Summary

| Epic | Module | Business Driver (BRD §7) | Release Span | Story Count | Primary Personas |
|---|---|---|---|---|---|
| EPIC-AST | Asset Management | BR-01, BR-14 | R1 (core) / R2 (15,16) | 16 | Elena, Marcus |
| EPIC-ORG | Organization Management | BR-02 | R1 | 6 | Priya, Marcus |
| EPIC-USR | User Management & RBAC | BR-11, BR-21 | R1 | 9 | Priya, Marcus |
| EPIC-SEC | Security | BR-07, BR-13 | R1 (core) / R2 (03b,07,12,13) | 17 | Priya, Officer Reyes |
| EPIC-MIG | Data Migration & Bulk Import/Export | BR-12 | R1 (01,03,04,05) / R3 (02) | 5 | Priya, Marcus |
| EPIC-CMP | Compliance & Data Privacy | BR-17, BR-18 | R1 | 6 | Officer Reyes |
| EPIC-SRC | Search | BR-03 | R1 (01–03) / R2 (04) / R3 (05) | 5 | Devon, Elena |
| EPIC-SCN | Scanning | BR-03 | R1 (01–05,07) / R2 (06) | 7 | Devon |
| EPIC-LIF | Asset Lifecycle Management | BR-04 | R2 | 16 | Elena, Dept Head, Sam |
| EPIC-AUD | Audit Management (core differentiator) | BR-05 | R2 | 24 | Devon, Dept Head, Grace |
| EPIC-INV | Inventory Management | BR-06 | R2 | 11 | Elena |
| EPIC-NTF | Notifications | BR-09 | R2 | 10 | Sam, all roles |
| EPIC-DSH | Dashboard | BR-08 | R2 | 7 | All roles, Board Viewer |
| EPIC-RPT | Reporting | BR-10 | R2 | 15 | Elena, Board Viewer, Grace |
| EPIC-INT | External Integrations | BR-16 | R1 (03) / R3 (01,02,04,05,06) | 6 | Priya, Officer Reyes, Board Viewer |
| EPIC-ANL | Product Analytics | BR-20 | R2 | 4 | Priya, Marcus, Sam |
| EPIC-PLAT | Platform & Non-Functional Enablers | BR-15, BR-19, §11.2 | Spans R1–R3 | 16 | Priya, IT/Infra Team |
| **Total** | | | | **180** | |

---

## 3. EPIC-AST — Asset Management

**Goal:** Maintain a complete, searchable register of individually tracked assets with unique identifiers, labels, custodianship, full history, and category-driven custom attributes (BR-01, BR-14). Register unchanged-from-v1.2 items (FR-AST-01–13) default to Must per §1 convention; FR-AST-14 ships Should-but-in-R1, FR-AST-15/16 are R2.

**US-AST-01 — Register a new asset with a generated identifier**
*FR-AST-01 · Must · R1*
As an Inventory Manager, I want to register a new asset against a category and its required fields, so that it receives a unique, trackable asset number the moment it enters the system.
- Given a valid category and all required fields, when I submit registration, then the asset is created with a system-generated `AST-YYYY-NNNNNN` number, `version 0`, and an `asset_created` history row, and barcode/QR values are returned (AC-AST-01-H).
- Given a required custom field for the category is blank, when I submit, then registration is rejected naming the missing field and nothing is created (AC-AST-01-X).

**US-AST-02 — Generate and print a scannable asset label**
*FR-AST-02 · Must · R1*
As an Inventory Manager, I want a printable barcode/QR label generated for every registered asset, so that I can physically tag it for later scanning.
- Given a registered asset, when I request its label, then it renders as Code 128 + QR (ECC ≥ M) at the configured size in PNG/SVG/PDF (AC-AST-02-H).
- Given the label printer is unreachable at intake, when registration completes, then the asset is still created and the label remains downloadable/printable later (AC-AST-02-X).

**US-AST-03 — Configure asset categories**
*FR-AST-03 · Must · R1*
As an Administrator, I want to define configurable asset categories and groups, so that data capture matches how our organization actually classifies assets, without code changes.
- Given a category is defined with a name and grouping, when saved, then it's available for selection at asset registration.
- Given a category is referenced by at least one asset, when its deletion is attempted, then it is blocked (409) with the dependent count (AC-AST-03-X).

**US-AST-04 — Model parent-child asset relationships**
*FR-AST-04 · Should · R1*
As an Inventory Manager, I want to link component assets to a parent asset, so that bundled equipment (e.g., a PC with monitor and dock) is tracked and disposed of coherently.
- Given a parent asset with two children, when the parent is transferred or disposed, then I am prompted to explicitly disposition each child (move-with-parent / detach / block) before the action completes.
- Given a parent's children, when an audit scopes the parent's location, then each child appears as its own distinct expected item, not merged into the parent's line.

**US-AST-05 — Attach images and files to an asset**
*FR-AST-05 · Must · R1*
As an Inventory Manager, I want to attach photos and documents (invoices, warranty cards) to an asset record, so that supporting evidence travels with the asset.
- Given a 2MB JPEG invoice, when I upload it to an asset, then its type/size/SHA-256 are stored, the binary lands only in the object store, and it streams back through the backend to authorized users (AC-AST-05-H).
- Given a 30MB file or an `.exe`, when upload is attempted, then it is rejected (413/415) and nothing is written to storage (AC-AST-05-X).

**US-AST-06 — Define per-category custom-field schemas**
*FR-AST-06 · Must · R1*
As an Administrator, I want to attach a custom-field schema (text/number/date/single-or-multi-select) to a category, so that category-specific data capture doesn't require a code change.
- Given a category with a required date field, when an asset in that category is created with a valid date, then the value persists in `custom_fields` and is filterable in advanced search (AC-AST-06-H).
- Given a text value is submitted for a date-typed field, when submitted, then it is rejected citing the offending `customFields.<name>` field (AC-AST-06-X).

**US-AST-07 — Configurable status lifecycle for assets**
*FR-AST-07 · Must · R1*
As an Inventory Manager, I want assets to move through a configurable status list (In Use, In Storage, Under Repair, Missing, Retired, Disposed, Void), so that current state is always visible and consistent org-wide.
- Given an asset In Storage, when I change status to In Use supplying the current `version`, then the change persists, `version` increments, and history records old→new (AC-AST-07-H).
- Given I submit with a stale `version`, when the update posts, then it is rejected with a conflict response carrying the current state — no change applies (AC-AST-07-X).

**US-AST-08 — Track warranty and AMC coverage**
*FR-AST-08 · Must · R1*
As an Inventory Manager, I want to record warranty and AMC period per asset, so that coverage windows are visible and nothing lapses unnoticed.
- Given a warranty end date, when saved, then the asset surfaces in the warranty-expiry report within the configured lookahead of that date (AC-AST-08-H).
- Given a warranty end before its start, when submitted, then the save is rejected (AC-AST-08-X).

**US-AST-09 — Record purchase and vendor details**
*FR-AST-09 · Must · R1*
As an Inventory Manager, I want to capture manufacturer, vendor, purchase date/cost, and PO reference on an asset, so that procurement provenance is always retrievable.
- Given complete purchase details, when saved, then they appear on the asset detail view and the asset register report (AC-AST-09-H).
- Given a negative purchase cost, when submitted, then it is rejected citing the `purchaseCost` field (AC-AST-09-X).

**US-AST-10 — See a complete, append-only change history per asset**
*FR-AST-10 · Must · R1*
As any authorized user, I want every change to an asset's status, location, assignment, or condition recorded as an immutable history entry, so that I can trust the asset's story has not been rewritten.
- Given any field change, when it commits, then an append-only row records the field, old/new value, actor, and timestamp (AC-AST-10-H).
- Given any actor including a Super Administrator, when they attempt to edit or delete a history row directly, then the API refuses it and points to the correct state-transition endpoint (AC-AST-10-X).

**US-AST-11 — Log asset movement between locations**
*FR-AST-11 · Must · R1*
As an Inventory Manager, I want every physical move of an asset between org nodes logged, so that I can answer "where has this been" and produce a movement report for any date range.
- Given an asset moved Room 204 → Room 310, when the move commits, then the movement log records from/to nodes, actor, and timestamp, and it appears in the movement report for that date range (AC-AST-11-H).
- Given a move targeting a nonexistent org node, when submitted, then it is rejected and no movement entry is created (AC-AST-11-X).

**US-AST-12 — Reserve an RFID identifier field for future use**
*FR-AST-12 · Should · R1*
As a Super Administrator, I want the asset schema to already carry an (unused) RFID tag field and abstraction point, so that a future RFID rollout doesn't require a data-model migration.
- Given the asset schema, when inspected, then an RFID identifier field exists and is nullable/unused in Phase 1.
- Given no RFID hardware is connected, when an asset is viewed, then the field is simply blank — no error, no forced entry.

**US-AST-13 — Bulk-import assets from a template**
*FR-AST-13 · Must · R1*
As an Inventory Manager, I want to bulk-import assets from a defined CSV/Excel template with pre-commit validation, so that migrating thousands of existing records doesn't require one-by-one entry. (Full flow owned by EPIC-MIG, US-MIG-03.)
- Given a valid 3,000-row template, when dry-run then commit runs, then valid rows create assets with generated numbers and a single reconciliation report shows created/rejected counts (AC-AST-13-H).
- Given 60 invalid rows in that batch, when committed after dry run, then only valid rows commit and all 60 rejections appear in the reconciliation report — nothing silently dropped (AC-AST-13-X).

**US-AST-14 — Track insurance policy details per asset**
*FR-AST-14 · Should (ships R1) · R1*
As an Inventory Manager, I want to record insurer, policy number, coverage amount, and expiry per asset, so that insurance lapses are visible before they happen.
- Given insurance details on an asset, when saved, then they persist and surface in the insurance-expiry report within its lookahead (AC-AST-14-H).
- Given an expiry date in the past, when saved, then it still saves, shown with a visible "expired" indicator rather than being rejected (AC-AST-14-X).

**US-AST-15 — Track vehicle-specific attributes**
*FR-AST-15 · Should · R2*
As an Inventory Manager, I want a Vehicle asset subtype with VIN, registration number, odometer, and registration/insurance expiry fields, so that fleet assets carry the attributes that actually matter for them.
- Given an asset assigned category "Vehicle," when I open its detail, then VIN/registration/odometer/expiry fields are available and persist.
- Given a non-vehicle asset, when viewed, then these fields are absent from the form — they don't clutter unrelated categories.

**US-AST-16 — Compute depreciation and net book value**
*FR-AST-16 · Should — required by FR-RPT-09 · R2*
As an Inventory Manager, I want depreciation parameters (method, useful life, salvage value) set per category with per-asset override, so that the system computes a depreciation schedule and net book value automatically instead of a manual spreadsheet.
- Given a category default of Straight-Line, 60 months, 10% salvage, and an asset's purchase cost/in-service date, when the depreciation schedule is computed, then monthly (full-month convention) net book value is available against the organization's configured fiscal year.
- Given an asset overrides useful life to 36 months, when computed, then the override — not the category default — drives that asset's schedule.

---

## 4. EPIC-ORG — Organization Management

**Goal:** Model the organization's physical and organizational structure and scope every record to it (BR-02).

**US-ORG-01 — Build a configurable multi-level org hierarchy**
*FR-ORG-01 · Must · R1*
As a Super Administrator, I want to build a multi-level hierarchy (e.g., Campus→Building→Floor→Room), so that every asset, audit, and user can be scoped to where it physically or organizationally belongs.
- Given I build Campus→Building→Floor→Room, when saved, then the tree persists, renders in tree view, and descendant queries return all nested assets (AC-ORG-01-H).
- Given a node with assets or users scoped to it, when its deletion is attempted, then it is blocked (409) with the dependent list (AC-ORG-01-X).

**US-ORG-02 — Relabel hierarchy level names**
*FR-ORG-02 · Must · R1*
As a Super Administrator, I want to rename generic level labels (e.g., "Campus" → "Parish") to match our sector's vocabulary, so that the system speaks our organization's language without a code change.
- Given level "Campus" relabeled to "Parish," when saved, then all UI and reports show "Parish" and existing data is unaffected (AC-ORG-02-H).
- Given a blank label is submitted, when saved, then it is rejected (AC-ORG-02-X).

**US-ORG-03 — Model Department/Cost Center independent of physical location**
*FR-ORG-03 · Must · R1*
As a Super Administrator, I want to define departments and cost centers as their own dimension, independent of building/room, so that budget-owning units that span or don't map to physical space are still trackable.
- Given a Department with a cost center and no physical node, when created, then assets and persons can reference it independently of location (AC-ORG-03-H).
- Given a department with assigned assets, when its deletion is attempted, then it is blocked with the dependent list (AC-ORG-03-X).

**US-ORG-04 — Maintain Person records independent of login accounts**
*FR-ORG-04 · Must · R1*
As an Administrator, I want to create a Person record for an employee or volunteer without requiring them to have a login, so that assets can be assigned to people who will never use the system directly.
- Given a volunteer who never logs in, when a Person record is created and an asset assigned to them, then the employee-asset report lists it with no user account required (AC-ORG-04-H).
- Given a Person with assigned assets, when erasure is requested, then it is blocked with the blocking-asset list until assets are reassigned (AC-ORG-04-X).

**US-ORG-05 — Scope every asset, audit, and user to a hierarchy node**
*FR-ORG-05 · Must · R1*
As a Department Head, I want my access automatically limited to my organization node and its descendants, so that I only ever see data relevant to my area, and so others can't see mine.
- Given a Department Head scoped to Science, when she lists assets, then only Science-scoped assets return (AC-ORG-05-H).
- Given the same user, when she fetches an out-of-scope asset by ID directly, then it is refused and a permission-denied entry is logged (AC-ORG-05-X).

**US-ORG-06 — Configure Classroom/Laboratory as Room variants**
*FR-ORG-06 · Should · R1*
As a Super Administrator, I want Classroom and Laboratory available as configurable variants of the Room level, so that education-sector deployments don't need a bespoke hierarchy model.
- Given the Room level, when I configure a "Classroom" variant, then rooms can be tagged as Classroom and filtered/reported as such.
- Given a deployment that doesn't need the variant, when left unconfigured, then Room behaves exactly as the base level — no forced complexity.

---

## 5. EPIC-USR — User Management & RBAC

**Goal:** Enforce role-based, org-scoped access on every protected action, and support safe, governed onboarding/offboarding and separation of duties (BR-11, BR-21).

**US-USR-01 — Provision a user with a role and org scope**
*FR-USR-01 · Must · R1*
As a Super Administrator, I want to create a user with one of the default roles and an org-scope restriction, so that access is correct from day one.
- Given I create a user with role Auditor and scope Building B, when she authenticates, then `GET /auth/me` shows role, scope, and computed permissions (AC-USR-01-H).
- Given an Administrator (not Super Admin) attempts to grant Super Administrator, when submitted, then it is refused — security-sensitive roles are Super-Admin-only to assign (AC-USR-01-X).

**US-USR-02 — Define custom roles with configurable permission sets**
*FR-USR-02 · Should · R1*
As a Super Administrator, I want to define a custom role with a specific set of permissions, so that a role that doesn't fit the nine defaults (e.g., a regional coordinator) can still be modeled precisely.
- Given a new custom role with a chosen permission set, when saved and assigned to a user, then that user's effective permissions match exactly what was configured — no more, no less.
- Given a custom role is in use by ≥1 user, when deletion is attempted, then it is blocked with the affected-user list.

**US-USR-03 — Enforce permission checks at API and UI for every protected action**
*FR-USR-03 · Must · R1*
As a Viewer, I should never be able to perform a write action, whether I try through the UI or by calling the API directly, so that read-only really means read-only.
- Given a Viewer, when she opens the asset register, then read succeeds and write controls are absent from the UI (AC-USR-03-H).
- Given the same Viewer crafts a direct write API call with her own token, when submitted, then it is refused server-side — UI hiding is never the control (AC-USR-03-X).

**US-USR-04 — Restrict access by organizational scope**
*FR-USR-04 · Must · R1*
As an Administrator, I want a user's role scope to filter every list, search, and detail fetch automatically, so that scoping is enforced consistently rather than screen-by-screen.
- Given a role scoped to Campus North, when any list/search runs, then results are limited to Campus North and its descendants (AC-USR-04-H).
- Given a user's scope is narrowed mid-session, when their next request arrives, then the new, narrower scope is already enforced (AC-USR-04-X).

**US-USR-05 — Scope the System Operator role to technical configuration only**
*FR-USR-05 · Must · R1*
As a Super Administrator, I want the System Operator role limited to technical configuration (backups, LDAP, system health), so that IT staff who manage infrastructure never see business or personal data they don't need.
- Given a System Operator, when she opens backup/LDAP/system-health settings, then access succeeds (AC-USR-05-H).
- Given the same System Operator requests asset valuations or person PII, when attempted, then it is refused (AC-USR-05-X).

**US-USR-06 — Block self-approval of one's own submissions**
*FR-USR-06 · Must · R1*
As an IT Security Officer, I want the system to prevent anyone from approving their own transfer, disposal, or audit submission, so that separation of duties is a real control, not a policy on paper.
- Given user A submitted a transfer, when user B (an authorized approver) approves it, then approval succeeds and records B's identity (AC-USR-06-H).
- Given user A submitted it, when A attempts to approve their own submission with no active waiver, then it is refused (AC-USR-06-X).

**US-USR-07 — Assign multiple flat roles instead of an inheriting hierarchy**
*FR-USR-07 · Should · R1*
As a Super Administrator, I want roles to be flat and non-inheriting — a user needing multiple capabilities gets multiple roles — so that "Administrator" never silently becomes "Inventory Manager plus more" and the SoD boundary stays real.
- Given a user needs both Inventory Manager and Auditor capabilities, when both roles are assigned, then their effective permissions are the union of both roles' explicit grants — nothing implied.
- Given a role's permission set changes, when saved, then only that role's assignees are affected — no cascading effect on other roles.

**US-USR-08 — Block offboarding while assets remain assigned**
*FR-USR-08 · Must · R1*
As an Administrator, I want deactivating a departing user blocked while they still hold assigned assets, so that offboarding never silently orphans equipment.
- Given a user with zero assigned assets, when deactivated, then status becomes Deactivated, sessions/refresh tokens revoke, and the event is logged (AC-USR-08-H).
- Given a user with 3 assigned assets, when deactivation is attempted, then it is blocked listing the 3 assets with resolution actions; once reassigned/returned, the same request succeeds (AC-USR-08-X).

**US-USR-09 — Record a Separation-of-Duties waiver for small organizations**
*FR-USR-09 · Must · R1*
As a Super Administrator at a single-administrator site, I want to record an SoD waiver signed off by the IT Security Officer, so that we can operate legitimately without being blocked by a control we're too small to fully separate — while keeping that exception auditable.
- Given a single-admin parish, when a waiver for AUDIT_APPROVAL is recorded with a resolvable IT Security Officer sign-off, then it activates and the SoD-conflict reroute path (US-AUD-22) engages on submission conflicts (AC-USR-09-H).
- Given a sign-off names a user without the IT Security Officer role, or names the requester themself, when submitted, then it is rejected — sign-off can never be self-asserted (AC-USR-09-X).

---

## 6. EPIC-SEC — Security

**Goal:** Authenticate every user securely and preserve a complete, immutable audit trail of all critical actions (BR-07, BR-13). FR-SEC-01/02/04/05/06/08/09 carry the FRS's implicit baseline priority of Must per §1 convention.

**US-SEC-01 — Authenticate with JWT access and refresh tokens**
*FR-SEC-01 · Must · R1*
As any user, I want to log in and stay signed in via a short-lived access token and revocable refresh token, so that my session is both convenient and safely revocable.
- Given valid credentials, when login succeeds, then a JWT access token and revocable refresh token issue, and refresh exchanges succeed until logout revokes them (AC-SEC-01-H).
- Given a refresh token already revoked by logout-all, when an exchange is attempted, then it is refused and the event logged (AC-SEC-01-X).

**US-SEC-02 — Authenticate via LDAP/AD without a hard dependency**
*FR-SEC-02 · Must · R1*
As a Super Administrator, I want to optionally connect LDAP/AD so staff log in with their existing directory credentials, but keep local login working if the directory is unavailable.
- Given LDAP is configured, when an LDAP user logs in via the standard login endpoint, then the backend delegates transparently and a local Super Admin fallback still works (AC-SEC-02-H).
- Given the LDAP server is down, when an LDAP user attempts login, then a clear "auth source unavailable" error returns and local-account login is unaffected (AC-SEC-02-X).

**US-SEC-03 — Mandatory MFA for Super Admin/Administrator**
*FR-SEC-03a · Must · R1*
As an IT Security Officer, I want MFA enrollment forced for every Super Administrator and Administrator, so that the highest-privilege accounts can't remain single-factor.
- Given a new Administrator account, when first login completes, then TOTP enrollment is forced before any other action (AC-SEC-03a-H).
- Given an Admin without completed MFA enrollment, when any other protected endpoint is called mid-enrollment, then it is refused until enrollment completes (AC-SEC-03a-X).

**US-SEC-04 — Maintain a complete, immutable activity log**
*FR-SEC-04 · Must · R1*
As an IT Security Officer, I want every login, permission change, export, and audit submission recorded in an append-only log, so that I have real evidence, not a claim, of what happened.
- Given any login, permission change, export, or audit submission, when it occurs, then an append-only row records actor, type, IP, and timestamp (AC-SEC-04-H).
- Given a failed login with an unknown username, when logged, then the row records the event with a null user ID and the API response never leaks whether that username exists (AC-SEC-04-X).

**US-SEC-05 — Enforce a configurable password policy**
*FR-SEC-05 · Must · R1*
As a Super Administrator, I want to set minimum length and complexity rules for passwords, so that weak credentials aren't a viable attack surface.
- Given a policy of min-12 plus complexity, when a compliant password is set, then it is accepted and stored hashed (bcrypt/Argon2) (AC-SEC-05-H).
- Given "password1" is submitted, when checked, then it is rejected citing the specific unmet rules (AC-SEC-05-X).

**US-SEC-06 — Enforce a configurable session timeout**
*FR-SEC-06 · Must · R1*
As a Super Administrator, I want idle sessions to time out after a configurable period, so that an unattended device doesn't remain a live session indefinitely.
- Given a 30-minute timeout, when a session idles 31 minutes, then the next request requires re-authentication, while an in-progress audit's offline scan queue survives the re-auth (AC-SEC-06-H).
- Given a step-up-required action (e.g., a permission change) with a fresh but non-step-up session, when invoked, then a step-up challenge is demanded first (AC-SEC-06-X).

**US-SEC-07 — Restrict access by IP range**
*FR-SEC-07 · Could · R2*
As a Super Administrator, I want to optionally restrict administrative access to known IP ranges, so that admin actions can't originate from arbitrary networks — while keeping a documented exemption path for mobile auditors off those ranges.
- Given IP restrictions enabled for the Administrator role, when a login attempts from an unlisted range, then it is refused with a clear message.
- Given a mobile Auditor role is exempted per the documented strategy, when they log in from a facility Wi-Fi outside the listed ranges, then login succeeds normally.

**US-SEC-08 — Encrypt sensitive data at rest and in transit**
*FR-SEC-08 · Must · R1*
As an IT Security Officer, I want personal and sensitive data encrypted at rest and every endpoint served over TLS, so that a storage or network compromise doesn't trivially expose data.
- Given person PII at rest, when storage is inspected, then values are encrypted and all endpoints serve over TLS (AC-SEC-08-H).
- Given a plain-HTTP request in a TLS-configured production deployment, when it reaches the proxy, then it is redirected or refused (AC-SEC-08-X).

**US-SEC-09 — Lock accounts after repeated failed logins**
*FR-SEC-09 · Must · R1*
As an IT Security Officer, I want an account to lock after 5 consecutive failed logins, with a logged, admin/self-service unlock, so that credential-guessing attacks are throttled.
- Given 5 consecutive failed logins, when the 5th fails, then the account locks for the cool-down, the lockout is logged, and unlock (admin or self-service) works (AC-SEC-09-H).
- Given a locked account, when correct credentials are supplied during the cool-down, then login is still refused with a lockout message (AC-SEC-09-X).

**US-SEC-10 — Export and erase/anonymize a person's data on request**
*FR-SEC-10 · Must · R1*
As a Compliance Officer, I want to export and then anonymize a departed person's personal data on request, so that data-subject rights are honorable in practice, subject to retention holds.
- Given a departed person with no holds, when a Compliance Officer runs erasure, then PII fields anonymize, a stable pseudonym reference persists in history, and an export was available beforehand (AC-SEC-10-H).
- Given an active legal hold on a linked audit, when anonymization is attempted, then it is blocked (423) and nothing changes (AC-SEC-10-X).

**US-SEC-11 — Search and filter the Security & Access Log**
*FR-SEC-11 · Must · R1*
As an Administrator, I want to search and filter the Security & Access Log by user, date, and event type, so that investigating an incident doesn't require a database query.
- Given an Administrator filters by user + date + type, when submitted, then matching entries return, paginated and exportable (AC-SEC-11-H).
- Given a Viewer requests the log, when attempted, then it is refused (AC-SEC-11-X).

**US-SEC-12 — Surface dependency-scan status to security roles**
*FR-SEC-12 · Should · R2*
As an IT Security Officer, I want to see the latest CI dependency-scan summary inside the app, so that I don't have to go find it in a separate CI system to know our exposure.
- Given a completed CI scan, when a Super Admin or IT Security Officer opens the security status view, then the latest scan summary (date, findings by severity) is visible.
- Given no scan has run yet, when the view loads, then it states that clearly rather than showing stale or blank data as if it were current.

**US-SEC-13 — Establish a patching, disclosure, and pen-test gate**
*FR-SEC-13 · Must (process) · R2*
As an IT Security Officer, I want a documented vulnerability patching/disclosure policy and a mandatory third-party penetration test before first production go-live, so that go-live isn't a security guess.
- Given the first production deployment is being planned, when go-live readiness is reviewed, then a completed penetration test report is a required, checked gate item.
- Given a vulnerability disclosure is received post-launch, when triaged, then it follows the documented patching/disclosure policy with defined response timelines.

**US-SEC-14 — Issue scoped service accounts for integrations**
*FR-SEC-14 · Must · R1*
As a Super Administrator, I want integrations to authenticate with narrowly-scoped service credentials rather than an admin login, so that a compromised integration can't act as a full administrator.
- Given an `INTEGRATION_SVC` credential scoped to `INT_ACCOUNTING_READ`, when it calls the depreciation export, then data returns; when it calls any other endpoint, then it is refused (AC-SEC-14-H).
- Given an attempt to assign the Integration Service role to a human user, when submitted, then it is rejected — the role is non-assignable to humans (AC-SEC-14-X).

**US-SEC-15 — Never store integration credentials in plaintext**
*FR-SEC-15 · Must · R1*
As an IT Security Officer, I want every integration's credentials stored only as a secrets-manager reference, never plaintext, so that a leaked config file or database dump can't hand over live credentials.
- Given an integration configured with credentials, when the stored row is inspected, then only a secrets-manager reference exists — no plaintext in DB, Compose files, or logs (AC-SEC-15-H).
- Given a config payload embeds a plaintext secret where a reference is required, when submitted, then it is rejected (AC-SEC-15-X).

**US-SEC-16 — Support time-boxed, notified "break-glass" emergency access**
*FR-SEC-16 · Must · R1*
As a Super Administrator, I want a documented emergency-access path that is time-boxed, reason-recorded, and dual-notified, so that a genuine emergency doesn't force me to bypass security controls invisibly.
- Given a Super Admin invokes break-glass with a reason, when activated, then elevated access lasts at most 4 hours, the IT Security Officer and one other Administrator are notified immediately, and every action in the window is flagged in the log (AC-SEC-16-H).
- Given no reason is supplied, when invocation is attempted, then it is rejected; given the window expires, then elevation ends automatically and the event stays flagged until reviewed (AC-SEC-16-X).

**US-SEC-17 — Offer optional MFA enrollment for roles not mandated**
*FR-SEC-03b · Could · R2*
> Added in v1.1 to close a traceability gap: FR-SEC-03b is individually tagged `[Could]` in the FRS (§2.12) and explicitly listed in both the Release Mapping Appendix (R2) and this document's own EPIC-SEC summary-table release-span note ("R2 (03b,07,12,13)"), but v1.0 never wrote its story — only FR-SEC-03a (US-SEC-03) existed.
As a user in a role where MFA isn't mandatory (e.g., an Auditor or Inventory Manager), I want to voluntarily enroll in TOTP MFA, so that I can opt into stronger account protection without waiting for a policy that forces it.
- Given a user whose role doesn't require MFA, when they open their security settings and enroll in TOTP MFA, then enrollment completes and subsequent logins require the second factor.
- Given a voluntarily-enrolled user chooses to disable it, when they do so after re-authenticating, then MFA is no longer required at their next login — Super Administrator/Administrator accounts are unaffected by this toggle and remain mandatorily enrolled per FR-SEC-03a (US-SEC-03).

---

## 7. EPIC-MIG — Data Migration & Bulk Import/Export

**Goal:** Migrate existing spreadsheet/legacy data safely with dry-run validation and reconciliation (BR-12).

**US-MIG-01 — Download bulk-import templates**
*FR-MIG-01 · Must · R1*
As an Inventory Manager, I want downloadable CSV/Excel templates for Asset, Person/Employee, Vendor, and Inventory Item, so that I know exactly what columns and formats are expected before I fill 3,000 rows.
- Given the Asset template is downloaded, when opened, then its columns match exactly what the dry-run validator checks — no guessing.
- Given a template version is superseded, when downloaded again, then the current version is served with its version marked.

**US-MIG-02 — Bulk-export data in a re-import-compatible format**
*FR-MIG-02 · Should · R3*
As a Super Administrator, I want to bulk-export the current asset/person/vendor register in the same format the importer accepts, so that data can move between environments or back into IAMS after external cleanup.
- Given a completed export job, when downloaded, then the file's structure round-trips cleanly through the same-module importer.
- Given an export is requested for a large dataset, when submitted, then it runs as a background job with progress, not a blocking request.

**US-MIG-03 — Dry-run validate, then commit, a bulk import with reconciliation**
*FR-MIG-03 · Must · R1*
As an Inventory Manager, I want to dry-run validate an import batch, see a per-row error report, and then explicitly commit only the valid rows, so that a bad file never silently corrupts the register.
- Given a completed dry run with 2,940 valid rows out of 3,000, when commit is called with an idempotency key, then 2,940 assets are created, the reconciliation report shows 2,940/0/60, and re-sending the same commit replays the cached result without duplicating (AC-MIG-03-H).
- Given the browser closes mid-commit, when I return, then the job continued server-side and its status/reconciliation are retrievable (AC-MIG-03-X).

**US-MIG-04 — View import run history**
*FR-MIG-04 · Must · R1*
As a Super Administrator, I want to see who ran each import, when, and with what outcome, so that a data-quality question can be traced to its source batch.
- Given three import runs, when the history is listed, then who/when/counts/outcome show for each, visible to Super Admin/Admin/IT Security Officer (AC-MIG-04-H).
- Given an Inventory Manager (unauthorized for this view) requests import history, when attempted, then it is refused (AC-MIG-04-X).

**US-MIG-05 — Run a rehearsed cutover with a documented rollback**
*FR-MIG-05 · Must (process) · R1*
As a Super Administrator, I want a migration cutover runbook — rehearsal import on a staging copy, agreed acceptance thresholds, and a documented restore-from-backup rollback — so that go-live migration isn't a one-shot gamble.
- Given a staging rehearsal import completes, when acceptance thresholds are checked against the runbook, then go/no-go for production cutover is a documented decision, not an ad hoc call.
- Given production cutover fails partway, when rollback is invoked, then restore-from-backup returns the system to its pre-cutover state per the documented step.

---

## 8. EPIC-CMP — Compliance & Data Privacy

**Goal:** Enforce configurable data-retention and legal-hold policies, and support data-subject export/erasure (BR-17, BR-18).

**US-CMP-01 — Configure per-entity-type retention policies**
*FR-CMP-01 · Must · R1*
As a Compliance Officer, I want to set a retention period and expiry action (delete/anonymize/hold-eligible) per entity type, so that data doesn't outlive its legitimate purpose — while never going below BRD §5.4's floors.
- Given a policy of `security_activity_log = 7 years, delete`, when the policy engine runs, then only rows older than 7 years and not under hold are purged, and the purge itself is logged (AC-CMP-01-H).
- Given a policy shorter than the BRD §5.4 floor, when saved, then it is rejected citing the floor (AC-CMP-01-X).

**US-CMP-02 — Anonymize a departed person's data through an approval workflow**
*FR-CMP-02 · Must · R1*
As a Compliance Officer, I want departed persons flagged at eligibility and anonymized only after my explicit approval, so that anonymization is deliberate, not automatic and irreversible by accident.
- Given a departed volunteer flagged eligible, when the Compliance Officer approves, then anonymization completes and historical audit findings still reference the stable pseudonym (AC-CMP-02-H).
- Given the person still has assets assigned, when approval is attempted, then it is blocked with the asset list (AC-CMP-02-X).

**US-CMP-03 — Configure privacy-notice text per personal-data field**
*FR-CMP-03 · Should · R1*
As a Compliance Officer, I want to attach configurable privacy-notice text to each personal-data field, so that data subjects see accurate, jurisdiction-appropriate notice at the point of capture.
- Given privacy-notice text configured for the "phone" field, when a Person record's phone field is displayed in a data-capture form, then that notice text renders alongside it.
- Given no notice text is configured for a field, when displayed, then the field renders normally with no broken placeholder.

**US-CMP-04 — Record accessibility-audit status**
*FR-CMP-04 · Should · R1*
As a Compliance Officer, I want to record the date and outcome of the latest WCAG 2.1 AA audit, so that accessibility compliance status is visible without digging through email threads.
- Given a completed accessibility audit, when its result is recorded, then it's visible on the compliance status view with date and outcome.
- Given no audit has yet been recorded, when the view loads, then it states that plainly rather than implying compliance.

**US-CMP-05 — Confirm data residency and flag outbound flows**
*FR-CMP-05 · Must · R1*
As a Compliance Officer, I want a single view confirming all data stores are on-premises and flagging any enabled outbound integration, so that I can answer a data-sovereignty question in seconds, not a meeting.
- Given no enabled outbound integrations, when the data-residency view loads, then it confirms all stores on-premises with zero flags (AC-CMP-05-H).
- Given `ACCOUNTING_EXPORT` is enabled, when the view loads, then that flow is flagged with its compliance-review record (AC-CMP-05-X).

**US-CMP-06 — Place a legal hold blocking retention purge and erasure**
*FR-CMP-06 · Must · R1*
As a Compliance Officer, I want to place a legal hold on an asset or audit record, so that litigation or investigation obligations override normal retention/anonymization timing.
- Given a legal hold on audit Q2-2026, when retention purge or anonymization touches it, then both are blocked (423) until the hold is lifted with a recorded reason (AC-CMP-06-H).
- Given a hold-lift attempted by someone who isn't a Compliance Officer or Super Admin, when submitted, then it is refused (AC-CMP-06-X).

---

## 9. EPIC-SRC — Search

**Goal:** Provide fast identification of any asset or stock item by scan or lookup (BR-03). FR-SRC-01–03 default to Must per §1 convention.

**US-SRC-01 — Run a global search across assets, vendors, and people**
*FR-SRC-01 · Must · R1*
As an Inventory Manager, I want one search box that finds matches across assets, vendors, and other entities, scoped to my permissions, so that I don't need to know which module holds what I'm looking for.
- Given "Latitude" matches assets and a vendor, when global search runs, then grouped, scope-filtered results return within performance targets at 100k assets (AC-SRC-01-H).
- Given a term with zero matches, when searched, then an explicit empty state returns — not an error (AC-SRC-01-X).

**US-SRC-02 — Look up an asset directly by its code**
*FR-SRC-02 · Must · R1*
As an Auditor, I want to type or scan an asset's code and jump straight to it, so that identification during a walkthrough is a single step.
- Given a scanned code `AST-2026-004821`, when looked up, then the single matching asset resolves within 1 second (AC-SRC-02-H).
- Given an unrecognized code, when looked up, then a not-found response returns with a "register this asset?" affordance for authorized roles (AC-SRC-02-X).

**US-SRC-03 — Combine advanced filters in a search**
*FR-SRC-03 · Must · R1*
As an Inventory Manager, I want to combine filters (category, status, location, date range) in one search, so that I can answer specific questions like "all IT equipment, In Use, in Building B, purchased this year."
- Given filters category=IT + status=In Use + Building B + a date range, when the filter tree posts, then the paginated result set returns only matching rows (AC-SRC-03-H).
- Given an unsupported sort field is requested, when submitted, then it is rejected (AC-SRC-03-X).

**US-SRC-04 — Save a frequently-used search**
*FR-SRC-04 · Should · R2*
As an Inventory Manager, I want to save a filter combination I use often, so that I don't have to rebuild it every time.
- Given a filter combination, when I save it with a name, then it appears in my saved-search list and re-applies with one click.
- Given a saved search referencing a since-deleted category, when re-applied, then it degrades gracefully (that filter clause is dropped, others still apply) rather than erroring out.

**US-SRC-05 — Reserve an RFID search field for future use**
*FR-SRC-05 · Could · R3*
As a Super Administrator, I want the search schema to already support an RFID identifier field, so that a future RFID rollout doesn't require a search re-architecture.
- Given the search index, when inspected, then an RFID field exists as a searchable (currently empty) attribute.
- Given no RFID data exists yet, when searching, then the field simply never matches — no error.

---

## 10. EPIC-SCN — Scanning

**Goal:** Provide fast identification of any asset or stock item by scan (BR-03). FR-SCN-01–05 default to Must per §1 convention.

**US-SCN-01 — Scan with a USB keyboard-wedge scanner**
*FR-SCN-01 · Must · R1*
As an Auditor, I want a USB barcode scanner in keyboard-wedge mode to just work with the scan input field, so that I don't need to install any driver.
- Given a USB scanner focused on the scan field, when a label is scanned, then the value resolves identically to typed input (AC-SCN-01/02-H).
- Given a scan of a non-IAMS barcode, when resolved, then it's handled as unrecognized without crashing (AC-SCN-01/02-X).

**US-SCN-02 — Scan with a Bluetooth HID scanner**
*FR-SCN-02 · Must · R1*
As an Auditor, I want a Bluetooth HID scanner to behave exactly like the USB one, so that facility choice of hardware doesn't change my workflow.
- Given a paired Bluetooth HID scanner focused on the scan field, when a label is scanned, then it resolves identically to USB/typed input (AC-SCN-01/02-H).
- Given the Bluetooth scanner disconnects mid-audit, when a scan is attempted, then a clear "not connected" state shows rather than a silent no-op (AC-SCN-01/02-X).

**US-SCN-03 — Scan with a phone/webcam camera**
*FR-SCN-03 · Must · R1*
As an Auditor without a dedicated scanner, I want to scan a barcode with my phone's camera in the browser, so that I can conduct an audit with only a phone.
- Given Chrome on Android or Safari on iOS, when camera scanning starts, then decode happens client-side and resolves within 1 second p95 on reference devices (AC-SCN-03-H).
- Given camera permission is denied, when scanning starts, then a clear permission prompt and fallback to manual entry appear — never a blank screen (AC-SCN-03-X).

**US-SCN-04 — Detect duplicate scans within a session**
*FR-SCN-04 · Must · R1*
As an Auditor, I want a re-scan of the same asset in one audit session flagged as a duplicate, so that I don't accidentally double-count it.
- Given asset X already scanned in this session, when scanned again, then DUPLICATE is signaled with a reference to the original scan and no double-count (AC-SCN-04-H).
- Given the same scan replays with the same idempotency key during offline sync, when received, then the cached response replays and counts stay unchanged (AC-SCN-04-X).

**US-SCN-05 — Resolve scans within one second under normal load**
*FR-SCN-05 · Must · R1*
As an Auditor, I want each scan to resolve to a result in about a second, so that a walkthrough of hundreds of assets doesn't turn into hours of waiting on a spinner.
- Given a normal on-prem network, when 100 sequential scans run, then p95 scan-to-display is at most 1 second (AC-SCN-05-H).
- Given the server is unreachable mid-scan, when scanning continues, then scans queue locally with a visible queue depth rather than silently failing (AC-SCN-05-X).

**US-SCN-06 — Provide an RFID-ready scan abstraction layer**
*FR-SCN-06 · Should · R2*
As a Super Administrator, I want the scan-input pipeline architected behind an abstraction that already anticipates RFID readers, so that adding RFID later is a driver, not a redesign.
- Given the scan-resolution service, when inspected, then it accepts a scan-source-agnostic input contract that barcode/QR scanning already uses.
- Given no RFID hardware exists yet, when the system runs, then nothing behaves differently — the abstraction is inert until used.

**US-SCN-07 — Configure barcode symbology and label sizes**
*FR-SCN-07 · Must · R1*
As a Super Administrator, I want to configure which barcode symbology and label sizes are supported (defaulting to Code 128 + QR at 50×25mm and 100×50mm), so that labels print correctly on the thermal stock we already own.
- Given the symbology endpoint is queried, when returned, then Code 128 + QR (ECC ≥ M) and configured label sizes come back, and labels print correctly on 50×25mm stock via standard OS printing (AC-SCN-07-H).
- Given an unsupported label size is requested, when submitted, then it is rejected listing supported sizes (AC-SCN-07-X).

---

## 11. EPIC-LIF — Asset Lifecycle Management

**Goal:** Manage the full asset lifecycle from purchase request through disposal/donation with approvals (BR-04). FR-LIF-01–10 default to Must per §1 convention (unchanged v1.2 baseline, not individually tagged in FRS 2.0).

**US-LIF-01 — Submit and approve a purchase request**
*FR-LIF-01 · Must · R2*
As an Inventory Manager, I want to submit a purchase request that routes to an approver, so that new procurement is authorized before money is committed.
- Given a complete purchase request, when submitted, then it routes to the configured approver and appears in their pending-approval queue.
- Given a request missing required justification, when submitted, then it is rejected before routing.

**US-LIF-02 — Create and link a purchase order to an approved request**
*FR-LIF-02 · Must · R2*
As an Inventory Manager, I want to create a PO from an approved purchase request and keep it linked, so that procurement traceability runs unbroken from request to receipt.
- Given an approved purchase request, when a PO is created from it, then the PO carries a link back to the originating request.
- Given a PO is created without an approved request behind it (where required by policy), when submitted, then it is blocked.

**US-LIF-03 — Reconcile received goods against the PO**
*FR-LIF-03 · Must · R2*
As an Inventory Manager, I want to reconcile what physically arrived against the PO, so that discrepancies (short-shipped, wrong item) are caught at intake, not months later.
- Given a PO for 10 units, when 10 units are received and reconciled, then the PO closes as fully received.
- Given only 8 of 10 arrive, when reconciled, then a discrepancy is recorded and the PO remains open for the remaining 2 (see US-LIF-16 for partial-receipt detail).

**US-LIF-04 — Assign an asset to a person or department**
*FR-LIF-04 · Must · R2*
As an Inventory Manager, I want to assign a received asset to a person or department, so that custodianship is recorded from the moment it enters service.
- Given a registered asset, when assigned to a Person, then the assignment is recorded, the person's asset list updates, and the affected person is notified.
- Given the asset is already assigned to someone else, when a new assignment is submitted, then the prior assignment is explicitly closed, not silently overwritten.

**US-LIF-05 — Transfer an asset with configured approval**
*FR-LIF-05 · Must · R2*
As a Department Head, I want an asset transfer between custodians or locations to require approval per policy, so that assets don't move without accountability.
- Given a transfer requiring approval, when submitted, then it routes to the configured approver and the asset shows "Transfer Pending" until decided.
- Given the approver rejects it, when the rejection is recorded, then the requester is notified with the reason and the asset remains with its current custodian.

**US-LIF-06 — Log a repair event**
*FR-LIF-06 · Must · R2*
As an Inventory Manager, I want to log when an asset goes out for repair and returns, so that downtime and repair cost history are trackable.
- Given an asset sent for repair, when logged, then status updates to Under Repair and the event records vendor, cost, and expected return.
- Given the asset returns from repair, when logged, then status reverts appropriately and the repair event closes with actual return date/cost.

**US-LIF-07 — Schedule and record preventive maintenance**
*FR-LIF-07 · Must · R2*
As an Inventory Manager, I want to schedule recurring preventive maintenance and record its completion, so that maintenance-critical assets (generators, HVAC) don't get missed.
- Given a maintenance schedule (e.g., every 6 months), when the due date approaches, then it surfaces on the maintenance-due dashboard/report and a reminder notification fires.
- Given maintenance is completed, when logged, then the next due date recalculates from the schedule.

**US-LIF-08 — Log corrective (unscheduled) maintenance**
*FR-LIF-08 · Must · R2*
As an Inventory Manager, I want to log unscheduled corrective maintenance separately from preventive maintenance, so that maintenance history distinguishes planned upkeep from reactive fixes.
- Given an unplanned maintenance event, when logged, then it's tagged Corrective and appears in maintenance history distinct from Preventive entries.
- Given a corrective event references a root-cause note, when saved, then the note persists and is visible on the maintenance history report.

**US-LIF-09 — Retire, dispose of, or donate an asset with reason and approval**
*FR-LIF-09 · Must · R2*
As an Inventory Manager, I want to retire, dispose of, or donate an asset only with a recorded reason, date, and approver, so that end-of-life removals are always accountable.
- Given a disposal request with reason and date, when approved, then status becomes Disposed and the event is immutable.
- Given the request lacks a reason, when submitted, then it is rejected before it reaches an approver.

**US-LIF-10 — Maintain an immutable lifecycle history per asset**
*FR-LIF-10 · Must · R2*
As a Read-only Auditor, I want every lifecycle event (purchase, assignment, transfer, repair, maintenance, disposal) recorded as an unalterable timeline, so that I can trust the asset's full story hasn't been edited after the fact.
- Given any lifecycle event, when it commits, then it appears as an append-only entry on the asset's lifecycle timeline.
- Given any actor attempts to edit or delete a past lifecycle event, when attempted, then it is refused; corrections are new linked records only.

**US-LIF-11 — Reject a lifecycle request with a mandatory reason**
*FR-LIF-11 · Must · R2*
As a Department Head, I want to reject a transfer or disposal request with a required reason, so that the requester understands why and can act on it instead of it just vanishing.
- Given a pending transfer, when I reject it with a reason, then the requester is notified with that reason and the request returns to Draft for revision.
- Given a rejection is attempted with no reason, when submitted, then it is blocked until a reason is supplied.

**US-LIF-12 — Restore a retired/disposed asset within a configurable window**
*FR-LIF-12 · Should · R2*
As an Administrator, I want to restore an asset that was retired or disposed in error, within a configurable window, so that a mistaken disposal doesn't permanently corrupt the register.
- Given an asset disposed 10 days ago within a 30-day restore window, when an Administrator restores it, then status reverts and a new immutable "restored" event is added — the original disposal event is not erased.
- Given the restore window has elapsed, when restoration is attempted, then it is blocked.

**US-LIF-13 — Escalate an approval after configurable inaction**
*FR-LIF-13 · Should · R2*
As an Inventory Manager, I want an approval that sits untouched for a configurable number of days to escalate automatically, so that a slow approver doesn't stall the whole process.
- Given an approval untouched for the configured inaction period, when the threshold passes, then it escalates in order: active delegate → Department Head's line manager → Administrator (per US-LIF-15).
- Given the approver acts before the threshold, when they approve or reject, then no escalation occurs.

**US-LIF-14 — Block erasure while assets remain assigned**
*FR-LIF-14 · Must · R2*
As a Compliance Officer, I want a person's erasure blocked while assets are still assigned to them, so that we never lose custodianship trail for equipment that's still out there.
- Given a person with 2 assigned assets, when erasure is requested, then it is blocked with the blocking-asset list.
- Given both assets are reassigned first, when erasure is retried, then it proceeds.

**US-LIF-15 — Delegate approval authority to a named alternate**
*FR-LIF-15 · Must · R2*
As a Department Head going on leave, I want to delegate my approval authority to a named alternate for a defined period, so that transfers and audits for my department don't stall while I'm away.
- Given a delegation set for a defined period, when a transfer needing my approval arrives during that window, then it routes to my delegate instead, and the delegation is logged.
- Given the delegation period ends, when a new approval arrives, then it routes back to me automatically.

**US-LIF-16 — Handle partial receipt, PO cancellation, and vendor returns**
*FR-LIF-16 · Should · R2*
As an Inventory Manager, I want to record partial PO receipt at line level, cancel/amend a PO before receipt with a reason, and log a return-to-vendor for rejected received items, so that real-world procurement edge cases don't force workarounds outside the system.
- Given a 10-unit PO line with 8 received, when reconciled, then the line shows 8 received / 2 outstanding as an immutable lifecycle record, and the PO stays open for the remainder.
- Given 2 received units are found defective, when a return-to-vendor is logged, then it produces its own immutable lifecycle record and those units are removed from active inventory.

---

## 12. EPIC-AUD — Audit Management (Core Differentiator)

**Goal:** Provide end-to-end physical audit capability with mobile scanning, evidence, immutability, approval, and certification — the product's core differentiator (BR-05). FR-AUD-01–18 default to Must per §1 convention (unchanged v1.2 baseline, not individually tagged in FRS 2.0).

**US-AUD-01 — Define an audit's type and scope**
*FR-AUD-01 · Must · R2*
As an Auditor, I want to define an audit's type (e.g., annual, spot-check) and its scope (org node, category, or asset list), so that every audit has a clear, bounded target before scanning starts.
- Given a scope of "Building B, IT Equipment," when the audit is created, then only matching assets become its expected-asset set.
- Given an empty scope selection, when creation is attempted, then it is blocked until at least one scoping criterion is set.

**US-AUD-02 — Assign one or more auditors to an audit**
*FR-AUD-02 · Must · R2*
As a Department Head, I want to assign one or more auditors to an audit, optionally splitting scope between them, so that a large audit can be completed in parallel.
- Given two auditors assigned to different sub-scopes of a bulk audit, when each scans their portion, then both contribute to the same audit's overall progress.
- Given an auditor's assignment ends (reassigned off the audit), when they attempt to scan, then it's refused and the assignment history retains their prior contribution.

**US-AUD-03 — Run bulk audits across a wide scope**
*FR-AUD-03 · Must · R2*
As an Auditor, I want to create a single audit spanning multiple locations or categories, so that an organization-wide sweep doesn't require managing dozens of separate audit records.
- Given a bulk audit scoped to an entire campus, when created, then its expected-asset set spans every matching asset across that campus.
- Given the bulk audit's progress is viewed, when opened, then it breaks down by sub-scope, not just one flat total.

**US-AUD-04 — Snapshot the expected-asset list at audit creation**
*FR-AUD-04 · Must · R2*
As an Auditor, I want the expected-asset list frozen at the moment the audit is created, so that what I'm supposed to find doesn't shift under me mid-walkthrough.
- Given an audit created against current scope, when created, then a fixed expected-asset snapshot is stored with it.
- Given an asset is added to the org node after the audit starts, when scanning continues, then that new asset is not added to this audit's expected set (US-AUD-23 governs the reverse case).

**US-AUD-05 — Verify a scanned asset with verifier, timestamp, and device**
*FR-AUD-05 · Must · R2*
As an Auditor, I want each scan recorded with who scanned it, when, and from which device, so that a verification claim is itself verifiable.
- Given a scan resolves to an expected asset, when recorded, then verifier identity, timestamp, and device ID are stored with it.
- Given a scan resolves to an asset outside this audit's scope, when recorded, then it's flagged OUT_OF_SCOPE rather than silently accepted.

**US-AUD-06 — Scan continuously without re-navigating between assets**
*FR-AUD-06 · Must · R2*
As an Auditor, I want to stay in a continuous scanning mode where each successful scan automatically readies the next, so that I can move through a room without tapping "next" every time.
- Given continuous scan mode is active, when one asset resolves, then the input is immediately ready for the next scan with no extra navigation step.
- Given I need to exit continuous mode to record a finding, when I do, then my place in the scan sequence is preserved on return.

**US-AUD-07 — Scan a batch and see results together**
*FR-AUD-07 · Must · R2*
As an Auditor, I want to scan a batch of assets and see the batch's resolved/unresolved results together, so that I can review a room's worth of scans at once rather than one at a time.
- Given 20 scans in a batch, when the batch completes, then a summary shows verified/duplicate/unrecognized counts for the batch.
- Given some scans in the batch fail to resolve, when reviewed, then those are called out distinctly, not buried in the success count.

**US-AUD-08 — See real-time expected-vs-verified progress**
*FR-AUD-08 · Must · R2*
As an Auditor, I want to see how many of the expected assets I've verified so far, live, so that I know how much scope remains.
- Given an audit with 200 expected assets, when 150 are verified, then the progress view shows 150/200 in real time as scans land.
- Given a scan is still queued offline (not yet synced), when progress is viewed, then it is reflected as pending, not silently omitted or double-counted once synced.

**US-AUD-09 — Classify unverified expected assets as Missing at closure**
*FR-AUD-09 · Must · R2*
As an Auditor, I want any expected asset never scanned during the audit window classified as Missing when I close it, so that unverified items don't just fall off the radar.
- Given 5 expected assets were never scanned, when the audit is closed, then those 5 are classified Missing and appear in the exception report.
- Given a Missing asset is later found, when reconciled outside the audit (US-AUD-21), then the closed audit's own record is not retroactively altered.

**US-AUD-10 — Flag damaged assets on a configurable condition scale**
*FR-AUD-10 · Must · R2*
As an Auditor, I want to record an asset's condition on a structured scale (Good/Fair/Minor Damage/Major Damage/Unusable), so that damage severity is comparable across audits rather than free-text guesswork.
- Given an asset scanned in Fair condition, when recorded, then the finding stores the exact scale value and it aggregates correctly in cross-audit condition reporting.
- Given a condition value outside the configured scale is submitted, when attempted, then it is rejected.

**US-AUD-11 — Attach photo evidence to a finding**
*FR-AUD-11 · Must · R2*
As an Auditor, I want to attach a photo directly to a finding at the moment I record it, so that damage or discrepancy evidence is captured in the moment, not reconstructed from memory later.
- Given a damaged asset finding, when a photo is attached, then it's checksummed, stored in the object store, and linked to that specific finding.
- Given the photo is captured offline, when connectivity resumes, then it syncs with the rest of the queued finding without loss (see EPIC-AUD FR-AUD-19).

**US-AUD-12 — Add remarks to a finding**
*FR-AUD-12 · Must · R2*
As an Auditor, I want to add a free-text remark to any finding, so that context that doesn't fit the structured fields (condition, photo) still gets captured.
- Given a finding, when a remark is added, then it persists and displays alongside the finding in the exception report.
- Given a remark exceeds the configured length limit, when submitted, then it is rejected with the limit stated.

**US-AUD-13 — Sign and submit a completed audit**
*FR-AUD-12 (signature clarification) · Must · R2*
As an Auditor, I want to submit my completed audit with an electronic signature (typed name plus credential re-authentication), so that submission carries real accountability, not just a button click.
- Given a completed audit ready for submission, when I re-authenticate (password or MFA) and confirm, then the signature record stores my name, user ID, timestamp, and the authentication event, and the audit moves to Pending Approval.
- Given re-authentication fails, when submission is attempted, then the audit remains editable and unsubmitted.

**US-AUD-14 — Route a submitted audit to Department Head approval with escalation**
*FR-AUD-13 · Must · R2*
As a Department Head, I want submitted audits for my scope to route to me for approval, escalating if I don't act in time, so that closure doesn't depend on catching me at my desk.
- Given a submitted audit for my department, when it arrives, then it appears in my pending-approval queue with a notification.
- Given I don't act within the configured window, when the threshold passes, then it escalates per US-LIF-13's resolution order.

**US-AUD-15 — Issue a completion certificate on closure**
*FR-AUD-14 · Must · R2*
As a Read-only Auditor, I want a completion certificate generated when an audit closes, so that I have a formal, exportable record of the audit's outcome for compliance purposes.
- Given an approved and closed audit, when closure completes, then a completion certificate (asset counts, verified/missing/damaged summary, approver, date) is generated and downloadable.
- Given the audit closes with open scope-change dispositions outstanding, when closure is attempted, then it's blocked until each is resolved (see US-AUD-23).

**US-AUD-16 — Generate an exception report per audit**
*FR-AUD-15 · Must · R2*
As a Department Head, I want a report listing everything that wasn't clean — Missing, Damaged, Out of Scope — for an audit, so that I can act on problems without re-reading every finding.
- Given a closed audit with 3 Missing and 2 Damaged assets, when the exception report is generated, then all 5 appear with their classification, evidence links, and remarks.
- Given an audit has zero exceptions, when the report is generated, then it states that clearly rather than returning an empty, ambiguous page.

**US-AUD-17 — View a live audit dashboard**
*FR-AUD-16 · Must · R2*
As a Department Head, I want a dashboard showing in-progress and recent audits for my scope, so that I can see audit health at a glance.
- Given active audits in my scope, when the dashboard loads, then progress %, exceptions, and pending approvals are visible without opening each audit individually.
- Given no audits are active in my scope, when the dashboard loads, then it shows a clear empty state, not a loading spinner that never resolves.

**US-AUD-18 — Analyze audit trends across cycles**
*FR-AUD-17 · Must · R2*
As an Inventory Manager, I want cross-cycle audit analytics (missing-rate trend, audit-prep time trend), so that I can show whether BO-2/BO-3 targets are actually moving, using system data rather than a manual tally.
- Given three completed audit cycles, when cross-cycle analytics are viewed, then trend lines for missing-rate and completion time render from system data (per BRD §1.3.1), not manual input.
- Given a missing asset was reconciled through the formal workflow (US-AUD-21), when analytics recompute, then that reconciliation — and only that path — counts toward the reduction.

**US-AUD-19 — Persist scans offline and sync without loss on reconnect**
*FR-AUD-19 · Must · R2*
As an Auditor working in a basement with poor connectivity, I want my scans (and any photo evidence) queued durably on my device and synced automatically once I'm back online, so that a connectivity gap never costs me re-work.
- Given the connection drops mid-audit, when I keep scanning, then scans and photos queue locally, persist across an app/browser restart, and show a visible queue depth with per-item sync status.
- Given connectivity resumes, when the queue syncs, then it does so idempotency-keyed — no loss, no double-count — even if sync is interrupted and retried.

**US-AUD-20 — Verify a large scope via statistical sampling**
*FR-AUD-20 · Should · R2*
As an Auditor covering a very large scope, I want an optional statistical sampling mode with a sample-size preview, so that a full 100% scan isn't the only option for huge inventories.
- Given a scope of 5,000 assets and a chosen confidence level, when sampling mode is selected, then a sample-size preview shows before scanning begins.
- Given sampling is left unselected, when the audit runs, then it defaults to 100% verification — sampling is never silently assumed.

**US-AUD-21 — Reconcile a previously-Missing asset outside an active audit**
*FR-AUD-21 · Must · R2*
As an Inventory Manager, I want to formally reconcile a Missing asset when it's later found, even outside any active audit, so that the correction is real and the closed audit's original finding is preserved, not edited.
- Given a Missing asset is found in Room 310, when reconciled, then a new linked record captures the find, updates the asset's status, and feeds analytics per BRD §1.3.1.
- Given the reconciliation attempts to edit the original Missing finding directly, when attempted, then it is refused — only a new linked record is permitted.

**US-AUD-22 — Detect and reroute Separation-of-Duties conflicts at submission**
*FR-AUD-22 · Must · R2*
As an Auditor, I want the system to catch it if I'm also the nominal approver for my own audit, so that I'm never put in a position to (accidentally or otherwise) self-approve.
- Given the nominal approver is the submitter and no waiver is active, when submission is attempted, then it is blocked with guidance to route to an alternate.
- Given an active SoD waiver covers this scope and action, when the same conflict occurs, then submission reroutes to the waiver-designated alternate automatically rather than blocking outright.

**US-AUD-23 — Handle assets that change scope mid-audit**
*FR-AUD-23 · Must · R2*
As an Auditor, I want an asset that gets transferred or disposed while my audit is still open flagged for explicit handling, so that a mid-audit change doesn't quietly produce a wrong Missing/Verified result.
- Given an expected asset is transferred to a new location mid-audit, when the transfer posts, then it's flagged "Scope Changed During Audit" pending disposition.
- Given the audit is closed with any scope-change disposition still open, when closure is attempted, then it's blocked until each is set to Confirm Verified at New Location, Exclude from Scope, or Accept as Exception.

**US-AUD-24 — Correct a recorded finding only via an immutable, linked correction record**
*FR-AUD-18 · Must · R2*
> Added in v1.1 to close a traceability gap: FR-AUD-18 ("immutability with corrections-as-linked-records," per FRS §2.5's unchanged-from-v1.2 block) is independently cited by the API Specification's `POST /audits/{id}/findings/{findingId}/corrections` endpoint, the Data Dictionary's `version` column note, the RTM, and the Personas doc, but had no corresponding story in v1.0. Numbered out of sequence (appended rather than inserted as US-AUD-13/14) to avoid renumbering every downstream cross-reference to US-AUD-13–23 elsewhere in this document and in the RTM.
As an Auditor assigned to the audit or an Administrator, I want to correct a mistake in a recorded finding only by adding a new linked correction record — never by editing the original in place — so that the audit trail stays tamper-evident even when the auditor themselves made the error.
- Given a finding with an incorrect condition value, when a correction is submitted via `POST /audits/{id}/findings/{findingId}/corrections`, then a new linked correction record is created carrying the old value, the new value, actor, and timestamp, and the original finding remains unchanged and visible alongside it.
- Given any actor, including a Super Administrator, attempts to edit or delete the original finding directly, when attempted, then the API refuses it and points to the corrections endpoint instead.

---

## 13. EPIC-INV — Inventory Management

**Goal:** Track quantity-based inventory with atomic stock movements, reorder alerts, valuation, and vendor history (BR-06).

**US-INV-01 — Track inventory quantity separately from individually tracked assets**
*FR-INV-01 · Must · R2*
As an Inventory Manager, I want consumable/bulk stock tracked by quantity rather than as individually numbered assets, so that items like paper or gloves don't force absurd one-by-one asset records.
- Given an inventory item defined for a stock category, when quantity is recorded, then it's tracked as a count, not as N individual asset records.
- Given the same item is later needed as an individually tracked asset (e.g., a serialized unit pulled from bulk stock), when that path is used instead, then it correctly creates a distinct AST record rather than conflating the two models.

**US-INV-02 — Record Stock In/Out with reason codes**
*FR-INV-02 · Must · R2*
As an Inventory Manager, I want every stock movement recorded with a reason code and the responsible user, so that "why did this quantity change" always has an answer.
- Given a Stock Out of 5 units for reason "Issued to Dept," when recorded, then quantity decreases by 5 and the transaction records reason and actor.
- Given a Stock Out would take quantity negative, when attempted, then it is rejected — quantity never goes below zero.

**US-INV-03 — Manage multiple warehouses with shelf/bin sub-locations**
*FR-INV-03 · Must · R2*
As an Inventory Manager, I want to track stock across multiple warehouses down to shelf/bin level, so that "where exactly is it" has a precise answer, not just "somewhere in the warehouse."
- Given an item stocked in Warehouse A, Aisle 3, Bin 12, when its location is viewed, then that full sub-location path displays.
- Given a warehouse is deactivated with stock still in it, when deactivation is attempted, then it is blocked until stock is moved out or transferred.

**US-INV-04 — Flag stock below a configurable reorder level**
*FR-INV-04 · Must · R2*
As an Inventory Manager, I want an item flagged automatically when its quantity crosses below a configured reorder level, so that I don't run out because nobody happened to check.
- Given a reorder level of 20 and current quantity 25, when a Stock Out drops it to 18, then the item is flagged low-stock and appears on the low-stock dashboard widget.
- Given quantity is replenished back above the reorder level, when updated, then the flag clears automatically.

**US-INV-05 — Record manual adjustments with mandatory reason and approver**
*FR-INV-05 · Must · R2*
As an Inventory Manager, I want a manual quantity adjustment (e.g., after a physical recount) to require a reason and an approver, so that quantities can't drift silently outside the normal Stock In/Out flow.
- Given a recount finds 3 fewer units than system quantity, when a manual adjustment is submitted with reason and routed for approval, then quantity changes only after approval.
- Given an adjustment is submitted with no reason, when submitted, then it is rejected before it reaches an approver.

**US-INV-06 — Value stock using a configurable costing method**
*FR-INV-06 · Should · R2*
As an Inventory Manager, I want stock valued using a configurable costing method (weighted average at minimum), so that valuation reports reflect a defensible accounting method, not just the last purchase price.
- Given multiple purchase lots at different unit costs, when weighted-average costing is configured, then the item's valuation report uses the weighted-average cost, recalculated on each receipt.
- Given the costing method is changed, when applied, then it takes effect prospectively and the change itself is recorded.

**US-INV-07 — Link vendor records to purchase history**
*FR-INV-07 · Must · R2*
As an Inventory Manager, I want a vendor's full purchase history visible from their record, so that I can evaluate a vendor's pricing/reliability without cross-referencing POs manually.
- Given a vendor with 12 historical purchase orders, when their record is opened, then all 12 are listed with date, item, quantity, and cost.
- Given a purchase order is created for that vendor, when saved, then it immediately appears in the vendor's history.

**US-INV-08 — Manage vendors and inter-warehouse transfers**
*FR-INV-08 · Must · R2*
As an Inventory Manager, I want full vendor CRUD (including deactivation) independent of items, and inter-warehouse transfers recorded as an atomic linked pair, so that vendor data management and stock movement between sites are both first-class, correct operations.
- Given a transfer of 10 units from Warehouse A to Warehouse B, when it commits, then both the deduction at A and the addition at B are recorded atomically as a linked transaction pair — never one without the other.
- Given a vendor with purchase history is deactivated, when deactivated, then it stops appearing in new-PO vendor pickers but its historical POs remain intact and visible.

**US-INV-09 — Register lot/batch stock with expiry visibility**
*FR-INV-09 · Should · R2*
As an Inventory Manager, I want to register stock by lot/batch with an optional expiry date, so that expiring consumables (first-aid supplies, chemicals) are visible before they go bad.
- Given a batch registered with an expiry date, when the configured lookahead window is reached, then it appears on the expiring-stock view.
- Given a batch has no expiry date (non-perishable), when registered, then it's tracked normally without ever appearing on the expiring-stock view.

**US-INV-10 — Capture multi-currency purchases with an entry-time FX rate**
*FR-INV-10 · Should · R2*
As an Inventory Manager, I want to record a purchase in its original currency along with the FX rate and date at entry, so that international vendor purchases report correctly in our reporting currency without recalculating rates retroactively.
- Given a purchase of €500 with an FX rate of 1.08 captured at entry, when saved, then the computed reporting-currency amount is stored and is what all aggregates use — it never recalculates against a later rate.
- Given no FX rate is available at entry, when submitted, then the system requires one before the purchase can be saved.

**US-INV-11 — Display a unit of measure on every stock transaction**
*FR-INV-11 · Should · R2*
As an Inventory Manager, I want every inventory item to carry a unit of measure (each, box, kg, litre), displayed on every transaction and report, so that "50" always means something specific.
- Given an item configured with UoM "box," when any Stock In/Out/adjustment is recorded, then the UoM displays alongside the quantity everywhere it appears.
- Given no automatic UoM conversion exists at this release, when a user tries to record a quantity in a different unit, then the system requires it in the item's configured UoM rather than silently converting incorrectly.

---

## 14. EPIC-NTF — Notifications

**Goal:** Notify users proactively of audits, expiries, low stock, and pending approvals (BR-09).

**US-NTF-01 — Send email notifications for catalog events**
*FR-NTF-01 · Must · R2*
As any user, I want to receive an email when a catalog event concerning me occurs (e.g., pending approval, upcoming audit), so that I don't have to keep IAMS open to stay informed.
- Given an event in the trigger catalog fires for me, when dispatched, then an email is sent using the versioned template for that event type.
- Given my email channel preference is off for that event type (and it's not Administrator-locked), when the event fires, then no email sends, but the in-app notification still does.

**US-NTF-02 — Send optional SMS via a configured gateway**
*FR-NTF-02 · Could · R2*
As an Auditor without reliable email access, I want critical notifications available via SMS through a configured gateway, so that time-sensitive alerts reach me even off email.
- Given an SMS gateway is configured and enabled for my account, when a security-alert-class event fires, then an SMS sends through the gateway.
- Given no SMS gateway is configured, when the same event fires, then the system degrades gracefully to email/in-app only — nothing errors.

**US-NTF-03 — Receive in-app notifications with read/unread state**
*FR-NTF-03 · Must · R2*
As any user, I want notifications inside the app with clear read/unread state, so that I have an always-available record even if email or SMS fails.
- Given a new notification, when it arrives, then it shows unread in my notification list and I can mark it read.
- Given email delivery for that event fails, when it fails, then the in-app notification still delivers — it's the always-available fallback channel.

**US-NTF-04 — Notify affected parties on assignment and transfer**
*FR-NTF-04 · Must · R2*
As an Employee/Volunteer, I want to be notified when an asset is assigned or transferred to or from me, so that I'm never surprised by custodianship I didn't know about.
- Given an asset is assigned to me, when the assignment commits, then I, the source approver, and the destination approver are all notified.
- Given a transfer is rejected, when rejected, then the requester and affected holder are notified with the reason.

**US-NTF-05 — Set per-event-type notification channel preferences**
*FR-NTF-05 · Must · R2*
As an Employee/Volunteer tired of irrelevant notifications, I want to control which channel I receive each event type on, so that I'm not nagged by things that don't matter to me — except where the Administrator has locked a type as mandatory.
- Given I turn off email for "low stock" (not applicable to my role's mandatory list), when saved, then I stop receiving that event by email but still see it in-app.
- Given an Administrator has locked "security alert" as mandatory, when I view my preferences, then that type is visibly non-editable.

**US-NTF-06 — Apply the standard event trigger catalog**
*FR-NTF-06 · Must · R2*
As a Super Administrator, I want the notification system to follow a standard, configurable trigger catalog (upcoming/overdue audit, expiries, maintenance due, low stock, pending approval, security alert), so that timing and recipients are consistent and tunable without custom code per event.
- Given the default catalog (e.g., upcoming audit at 7 and 1 days before), when those thresholds pass, then notifications fire to the catalog's default recipients exactly once per threshold.
- Given an overdue audit remains open, when the overdue trigger fires, then it repeats every 3 days until the audit closes, per the catalog's repeat rule.

**US-NTF-07 — Resolve notification recipients by role × scope at send time**
*FR-NTF-07 · Must · R2*
As a Department Head, I want "Dept Head of scope" recipients resolved dynamically at send time (honoring any active delegation), so that I get the right notifications even after an org change, without manual recipient list maintenance.
- Given "Dept Head of scope" as a catalog recipient rule, when an event fires for Science department, then it resolves through the department hierarchy to whoever currently holds that role — including an active delegate.
- Given the Dept Head role reassigns to a new person, when the next event fires, then the new holder receives it — no stale recipient list.

**US-NTF-08 — Track delivery and retry failed notifications**
*FR-NTF-08 · Must · R2*
As a Super Administrator, I want failed notification deliveries retried automatically and, if they keep failing for an approval-class event, escalated to an in-app alert, so that a bounced email doesn't silently strand an approval.
- Given an email delivery fails, when retried, then it retries with exponential backoff up to the default 3 attempts, each attempt logged with status.
- Given all retries for an approval-class notification fail, when exhausted, then an in-app alert raises to an Administrator.

**US-NTF-09 — Customize notification templates per event and channel**
*FR-NTF-09 · Should · R2*
As a Super Administrator, I want versioned, variable-substituted templates per event type and channel, so that notification wording can be tuned without a code deployment.
- Given a template edited for "low stock" email, when saved as a new version, then subsequent sends use the new version while past-sent notifications remain as originally sent.
- Given a template variable (e.g., item name) isn't supplied at send time, when rendered, then it fails safely with a clear placeholder rather than sending a broken message.

**US-NTF-10 — Deep-link a notification through login back to its resource**
*FR-NTF-10 · Must · R1*
As a Department Head, I want clicking an approval link in an email to take me through login (honoring SSO/MFA) straight to that approval, so that an expired session never dead-ends me on a generic homepage.
- Given an approval email deep link and an expired session, when I click it, then login intervenes and I land on the exact approval afterward (AC-NTF-10-H).
- Given a deep link points to a resource outside my scope, when followed post-login, then I get a safe landing page with a clear message — never a broken route (AC-NTF-10-X).

---

## 15. EPIC-DSH — Dashboard

**Goal:** Provide role- and scope-filtered dashboards of assets, audits, stock, and expirations (BR-08). FR-DSH-01–07 default to Must per §1 convention.

**US-DSH-01 — See asset totals and category breakdowns**
*FR-DSH-01 · Must · R2*
As an Inventory Manager, I want a dashboard tile showing total assets and their breakdown by category/status, so that I get the big picture without running a report.
- Given assets across 5 categories, when the dashboard loads, then totals and per-category breakdown render, scoped to my org access.
- Given the underlying data changes, when I reload within the caching window, then I may see up to 5 minutes of staleness, per the documented cache policy — never longer.

**US-DSH-02 — See audit completion percentage**
*FR-DSH-02 · Must · R2*
As a Department Head, I want to see what percentage of active audits in my scope are complete, so that I can tell at a glance if we're on track.
- Given 3 active audits at 40%, 80%, and 100% completion, when the dashboard loads, then the audit-completion widget reflects each with near-real-time staleness (≤30 seconds).
- Given no active audits exist in my scope, when the dashboard loads, then the widget shows a clear empty/complete state.

**US-DSH-03 — See upcoming expirations and maintenance due**
*FR-DSH-03 · Must · R2*
As an Inventory Manager, I want a widget listing upcoming warranty/AMC/insurance expirations and maintenance due dates, so that lapses don't sneak up on me.
- Given items expiring within the configured lookahead, when the dashboard loads, then they list sorted by nearest date.
- Given nothing is due within the lookahead, when the dashboard loads, then the widget shows a clean "nothing due" state.

**US-DSH-04 — See items below reorder level**
*FR-DSH-04 · Must · R2*
As an Inventory Manager, I want a low-stock widget showing every item currently below its reorder level, so that replenishment decisions don't wait for someone to notice a shelf is empty.
- Given 4 items below reorder level, when the dashboard loads, then all 4 list with current quantity and reorder threshold.
- Given an item crosses back above its reorder level, when the dashboard reloads, then it drops off the widget automatically.

**US-DSH-05 — See an activity feed and audit calendar**
*FR-DSH-05 · Must · R2*
As a Department Head, I want a recent-activity feed and an audit calendar view, so that I can see what's happened and what's scheduled without navigating multiple screens.
- Given recent transfers, approvals, and audit events in my scope, when the dashboard loads, then they list chronologically in the activity feed.
- Given audits scheduled in the next 30 days, when the calendar view loads, then each appears on its scheduled date.

**US-DSH-06 — Configure which KPIs appear on my dashboard**
*FR-DSH-06 · Must · R2*
As a Viewer, I want to choose which KPI tiles appear on my dashboard, so that the view matches what I actually care about rather than a fixed, generic layout.
- Given a set of available KPI tiles, when I select a subset and save, then my dashboard renders only those tiles on next load.
- Given I haven't configured anything yet, when I first load the dashboard, then a sensible role-appropriate default set renders.

**US-DSH-07 — See only data within my role and org scope**
*FR-DSH-07 · Must · R2*
As a Department Head, I want my dashboard automatically filtered to my role and org scope, so that I never see — or worry I'm missing — data outside my authority.
- Given I'm scoped to Building B, when any dashboard widget loads, then every figure reflects Building B and its descendants only.
- Given my scope changes, when the dashboard next loads, then it reflects the new scope immediately.

---

## 16. EPIC-RPT — Reporting

**Goal:** Provide standard reports exportable to PDF/Excel/CSV with scheduling (BR-10). FR-RPT-01–14 default to Must per §1 convention (unchanged v1.2 baseline, not individually tagged in FRS 2.0); FR-RPT-15 is explicitly Could.

**US-RPT-01 — Generate a full asset register report**
*FR-RPT-01 · Must · R2*
As a Viewer, I want a complete asset register report, so that I can present an accurate inventory position without asking IT to pull one together.
- Given the current asset register, when the report is generated, then every in-scope asset appears with its key attributes.
- Given the register exceeds a page-renderable size, when generated, then it streams/paginates rather than timing out.

**US-RPT-02 — Generate department/room/building inventory reports**
*FR-RPT-02 · Must · R2*
As a Department Head, I want an inventory report scoped to my department, room, or building, so that I can answer "what do we actually have" for my area specifically.
- Given a scope selection of "Science Department," when generated, then only Science-scoped assets appear.
- Given the scope has zero assets, when generated, then the report states that clearly rather than erroring.

**US-RPT-03 — Generate an employee asset list report**
*FR-RPT-03 · Must · R2*
As an Administrator, I want a report of everything assigned to a given employee or volunteer, so that an offboarding conversation starts with facts, not guesswork.
- Given a Person with 4 assigned assets, when the report is generated for them, then all 4 list with assignment date.
- Given the Person has zero assignments, when generated, then it shows an explicit empty result.

**US-RPT-04 — Generate a missing/lost/damaged report**
*FR-RPT-04 · Must · R2*
As an Inventory Manager, I want a consolidated report of every Missing, Lost, or Damaged asset, so that I can track loss trends without re-running audit exception reports one by one.
- Given assets across multiple audits in Missing/Damaged status, when generated, then all appear together with source audit reference.
- Given a filter by date range is applied, when generated, then only matching-period items appear.

**US-RPT-05 — Generate warranty, AMC, and insurance expiry reports**
*FR-RPT-05 · Must · R2*
As an Inventory Manager, I want a single report of upcoming warranty, AMC, and insurance expirations within a lookahead window, so that renewal decisions happen before coverage lapses, not after.
- Given assets with warranty/AMC/insurance expiring within the configured lookahead, when generated, then all three categories appear together, sorted by nearest expiry.
- Given the lookahead window is changed, when regenerated, then results reflect the new window.

**US-RPT-06 — Generate purchase and vendor reports**
*FR-RPT-06 · Must · R2*
As an Inventory Manager, I want purchase-history and vendor-performance reports, so that procurement decisions are backed by real spend and reliability data.
- Given purchase orders across multiple vendors, when the report is generated, then totals and item-level detail break down by vendor.
- Given a date range filter is applied, when generated, then only orders within that range appear.

**US-RPT-07 — Generate an asset movement report**
*FR-RPT-07 · Must · R2*
As an Inventory Manager, I want a report of asset movements between locations over a date range, so that I can audit physical relocation activity without opening each asset's history individually.
- Given movements logged across multiple assets in a date range, when generated, then each movement's from/to nodes, actor, and timestamp appear.
- Given no movements occurred in the range, when generated, then the report states that clearly.

**US-RPT-08 — Generate audit compliance and summary reports**
*FR-RPT-08 · Must · R2*
As a Read-only Auditor, I want an audit compliance/summary report across a period, so that I can assess overall audit health without opening every individual audit.
- Given multiple audits completed in a quarter, when the report is generated, then completion rate, exception rate, and on-time rate summarize across them.
- Given some audits in the period are still open, when generated, then they're clearly distinguished from closed ones, not counted as complete.

**US-RPT-09 — Generate a depreciation report**
*FR-RPT-09 · Must · R2*
As a Viewer, I want a depreciation report using the parameters set on each asset/category (FR-AST-16), so that I have defensible net book value figures for board reporting without a separate spreadsheet.
- Given assets with depreciation parameters set, when the report is generated, then net book value per asset computes from the stored schedule, as of the report date.
- Given an asset has no depreciation parameters configured, when generated, then it appears clearly flagged as "not depreciated" rather than showing a misleading zero.

**US-RPT-10 — Generate a maintenance history report**
*FR-RPT-10 · Must · R2*
As an Inventory Manager, I want a maintenance history report across assets, so that I can spot which assets are becoming maintenance liabilities.
- Given preventive and corrective maintenance events across assets, when the report is generated, then both types appear with cost and downtime where recorded.
- Given filtered to a single asset, when generated, then only that asset's maintenance timeline appears.

**US-RPT-11 — Print labels in batch**
*FR-RPT-11 · Must · R2*
As an Inventory Manager, I want to select a set of assets and print all their labels in one batch job, so that a large intake doesn't mean printing labels one at a time.
- Given 50 selected assets, when batch label printing is requested, then a single print-ready file with all 50 labels at the configured size is produced.
- Given one of the 50 assets has no valid label data, when requested, then that one is flagged and excluded rather than failing the whole batch.

**US-RPT-12 — Export any report to PDF, Excel, or CSV**
*FR-RPT-12 · Must · R2*
As a Viewer, I want any report exportable to PDF, Excel, or CSV, so that I can bring the data into whatever tool the board meeting actually uses.
- Given a generated report, when I choose Excel export, then a correctly formatted .xlsx downloads matching the on-screen data.
- Given a very large report export, when requested, then it runs as a background job with progress rather than blocking the UI.

**US-RPT-13 — Schedule a report for recurring delivery**
*FR-RPT-13 · Must · R2*
As an Inventory Manager, I want to schedule a report to run and email itself on a recurring basis, so that a standing monthly report doesn't require me to remember to run it.
- Given a report scheduled monthly to 3 recipients, when the scheduled time arrives, then it generates and delivers automatically to all 3.
- Given a scheduled report's owner is deactivated, when the next run is due, then the schedule pauses and flags for reassignment rather than failing silently forever.

**US-RPT-14 — Generate a Security & Access Log report**
*FR-RPT-14 · Must · R2*
As an Administrator, I want the Security & Access Log exportable as a formal report, so that I can hand evidence to an auditor or reviewer without a screen-share of the live log.
- Given a filtered Security & Access Log view, when exported, then the export matches the filtered set exactly.
- Given a Viewer attempts to access this report, when attempted, then it is refused, consistent with US-SEC-11.

**US-RPT-15 — Build an ad hoc, saved report**
*FR-RPT-15 · Could · R2*
As an Inventory Manager, I want to build a custom report from available fields and filters and save it for reuse, so that one-off analytical questions don't require a development request.
- Given a chosen set of fields and filters, when built and saved, then it appears in my saved-reports list and reruns with current data on demand.
- Given a saved ad hoc report references a field later removed from the schema, when rerun, then it degrades gracefully with that column omitted and a note, not a hard failure.

---

## 17. EPIC-INT — External Integrations

**Goal:** Support governed, read-only external data exports and identity-provider integrations, disabled by default (BR-16).

**US-INT-01 — Export depreciation and valuation data read-only to accounting**
*FR-INT-01 · Must · R3*
As a Viewer, I want a read-only, stable-schema export of depreciation and valuation data for accounting/ERP consumption, so that finance can ingest asset figures without a live bidirectional integration risk.
- Given the export is enabled and reviewed, when run, then a CSV/JSON file in the documented, stable column schema is produced with no write path back into IAMS.
- Given the schema changes in a future version, when released, then it's versioned so existing accounting-side imports don't silently break.

**US-INT-02 — Sync HR/SIS rosters read-only**
*FR-INT-02 · Should · R3*
As a Super Administrator, I want to pull employee/student roster data from HR/SIS on demand or on a schedule, so that Person records stay current without manual re-entry.
- Given a configured HR/SIS source, when a sync runs (manual or scheduled), then new/changed roster records update Person records, one-directionally.
- Given the source is unreachable at sync time, when attempted, then the sync fails cleanly with a logged error — it never partially applies a corrupt batch.

**US-INT-03 — Authenticate via LDAP/AD or SSO (SAML2/OIDC)**
*FR-INT-03 · Must · R1*
As any user in an organization with an identity provider, I want to log in through our existing SSO, so that I don't need a separate IAMS-only password to manage.
- Given a configured OIDC provider, when a user completes the SSO flow, then an IAMS session issues mapped to their account; LDAP-delegated login also works through the standard login endpoint (AC-INT-03-H).
- Given an invalid or expired IdP assertion, when the callback fires, then the session is refused and the failure logged — no partial session (AC-INT-03-X).

**US-INT-04 — Deliver outbound webhooks to registered, allow-listed URLs**
*FR-INT-04 · Should · R3*
As a Super Administrator, I want to register specific outbound webhook URLs and event types, so that external systems can react to IAMS events without IAMS accepting arbitrary callback destinations.
- Given a registered, allow-listed webhook URL for "asset.disposed," when that event fires, then a delivery attempt is made and logged with retries on failure.
- Given a request body attempts to specify an ad hoc callback URL instead of using a registered one, when submitted, then it is rejected — only Administrator-registered URLs are ever used.

**US-INT-05 — Require Compliance review before enabling any outbound integration**
*FR-INT-05 · Must · R3*
As a Compliance Officer, I want every integration disabled by default and unable to be enabled without my recorded review, so that no outbound data flow starts without governance sign-off — enforced in software, not just process.
- Given a newly configured integration, when an attempt is made to enable it without a recorded Compliance Officer review, then it is blocked.
- Given the Compliance Officer records their review and approves, when enabling is retried, then it succeeds and the enable event is logged.

**US-INT-06 — Sign webhook payloads for receiver verification**
*FR-INT-06 · Must (with FR-INT-04) · R3*
As an external system receiving IAMS webhooks, I want each payload signed with HMAC-SHA256 over the raw body using a per-webhook secret, so that I can verify the payload really came from IAMS and wasn't tampered with.
- Given a webhook delivery, when sent, then it carries an HMAC-SHA256 signature header computed over the raw body with that webhook's secret.
- Given a receiver recomputes the signature, when verifying, then constant-time comparison is documented as the required verification method to avoid timing side-channels.

---

## 18. EPIC-ANL — Product Analytics

**Goal:** Capture deployment-local usage analytics and user feedback; never transmit outside the deployment (BR-20).

**US-ANL-01 — Capture feature-usage events server-side**
*FR-ANL-01 · Should · R2*
As a Super Administrator, I want feature-usage events captured server-side, so that I can understand actual adoption without relying on a third-party analytics SDK.
- Given a user performs a tracked action (e.g., runs a report), when it completes, then a usage event is recorded server-side with module, action, and role — no client-side tracking script involved.
- Given the capture pipeline is unavailable, when an action occurs, then the business action still completes normally — analytics capture never blocks a user-facing operation.

**US-ANL-02 — Guarantee analytics never leave the deployment**
*FR-ANL-02 · Must · R2*
As a Compliance Officer, I want it structurally impossible for usage analytics to transmit outside our deployment, so that BO-5 (data sovereignty) isn't just a policy statement.
- Given the analytics module, when inspected, then there is deliberately no client-side event-submission API and no outbound network call in its code path.
- Given the data-residency view (US-CMP-05) is checked, when reviewed, then analytics data is confirmed on-premises alongside every other store.

**US-ANL-03 — View a usage adoption report**
*FR-ANL-03 · Should · R2*
As a Super Administrator, I want a report of feature adoption by role and module frequency, so that I can see where training or UX attention is actually needed.
- Given 90 days of captured usage events, when the adoption report is generated, then it breaks down usage by role and by module.
- Given a role has near-zero usage of a module they're expected to use, when viewed, then that gap is visible, not averaged away.

**US-ANL-04 — Submit in-app feedback**
*FR-ANL-04 · Should · R2*
As an Employee/Volunteer, I want to submit a quick category-plus-free-text feedback item from within the app, so that friction I notice reaches someone without a separate email or ticket.
- Given a feedback submission with category and text, when submitted, then it routes to the configured recipient and confirms receipt to me.
- Given the free-text field is left empty but a category is chosen, when submitted, then it's accepted — category alone is still useful signal.

---

## 19. EPIC-PLAT — Platform & Non-Functional Enablers

**Goal:** Deliver the SRS 2.0 architecture and NFR commitments that have no direct FR but are load-bearing for every module above — operational simplicity (BRD §11.2), on-premises operation (BR-15), accessibility (BR-19), and the new CONC/OBS/API/MAINT NFR families. These are engineering-facing enabler stories, typically delivered incrementally alongside the module stories that depend on them rather than as one-off tickets.

**US-PLAT-01 — Deploy the full stack with a single command**
*SRS §2.4, NFR-DEPLOY-01 · Must · R1*
As an IT-generalist administrator, I want the entire stack (backend, frontend, database, object store, reverse proxy) to come up with one `docker compose up`, so that deployment doesn't require a specialist.
- Given a Docker-capable host sized per the Installation Guide, when `docker compose up` runs, then all five services start healthy with no manual post-start steps beyond configuration.
- Given a container fails to start, when checked, then its logs clearly identify the cause — no silent partial-stack state.

**US-PLAT-02 — Store all binary content in an object store, brokered by the backend**
*SRS §2.4/§2.5/§6, NFR-SEC-10, NFR-AVAIL-06 · Must · R1*
As a Super Administrator, I want asset attachments, audit evidence, and signature records stored in MinIO — never directly reachable by clients — and validated before write, so that binary content is secure, checksummed, and never bypasses authorization.
- Given a file upload, when it's written, then content-type/size are validated server-side before any object-store write, the object commits first, and its DB metadata row commits second (orphans reaped by a scheduled janitor job).
- Given a client attempts to reach the object-store container directly, when attempted, then it's unreachable — all attachment traffic is brokered through the backend with the parent entity's authorization.

**US-PLAT-03 — Back up and restore both the database and object store together**
*SRS §4.3, NFR-AVAIL-02/04 · Must · R1*
As an IT/Infrastructure Team member, I want automated backups covering both PostgreSQL and MinIO, with a tested restore procedure for the pair, so that a disaster doesn't leave evidence photos orphaned from their metadata.
- Given a configured backup schedule, when it runs, then both the database and object store are captured together as a coherent pair.
- Given a restore drill is executed, when completed, then RPO ≤24h and RTO ≤8h are met for a single-node deployment, and restored evidence photos retain their correct metadata linkage.

**US-PLAT-04 — Reject stale writes on mutable entities via optimistic locking**
*SRS §4.5, NFR-CONC-01 · Must · R1/R2 (per entity)*
As an Inventory Manager, I want a stale edit to a shared record (asset, audit, finding, inventory item) rejected with the current state rather than silently overwriting someone else's concurrent change, so that two people editing the same record never clobber each other invisibly.
- Given two users load the same asset simultaneously and the first saves, when the second submits their (now-stale) `version`, then it's rejected with a conflict response carrying current state.
- Given the second user reloads and resubmits with the current `version`, when submitted, then it succeeds.

**US-PLAT-05 — Guarantee atomic inventory quantity mutations under concurrency**
*SRS §4.5, NFR-CONC-02 · Must · R2*
As an Inventory Manager, I want simultaneous stock-out requests against the same item to never both succeed past zero, so that concurrent warehouse activity can't produce impossible negative stock.
- Given two simultaneous Stock Out requests that together exceed available quantity, when both submit, then the row-level atomic operation allows only what's actually available and rejects/queues the rest — never a negative-quantity result.
- Given a client retries a stock mutation after a timeout, when retried with the same idempotency key, then it's safely deduplicated rather than double-applied.

**US-PLAT-06 — Emit structured, correlated logs across every request**
*SRS §4.6, NFR-OBS-01/03 · Must · R1*
As an IT/Infrastructure Team member, I want every request to carry a correlation ID surfaced end-to-end in logs and error responses, so that diagnosing a reported problem doesn't mean grepping blind through unstructured logs.
- Given any API request, when processed, then a `traceId` is generated (or propagated), appears in structured JSON logs at every layer touched, and is returned in any error response.
- Given PII appears in a logged field, when written, then it's redacted per CMP tagging rather than logged in the clear.

**US-PLAT-07 — Expose operational metrics and an admin status page**
*SRS §4.6, NFR-OBS-02 · Should · R2*
As a Super Administrator, I want a built-in status page showing request rate, error rate, p95 latency, queue depth, and disk headroom with threshold warnings, so that I catch operational trouble before users do.
- Given the admin status page, when opened, then current metrics render with clear thresholds (e.g., disk >85% flagged).
- Given the outbox/job queue depth grows abnormally, when checked, then it's visibly flagged rather than buried in a raw metrics endpoint only ops tooling reads.

**US-PLAT-08 — Rate-limit API requests per user and per integration key**
*SRS §4.7, NFR-API-01/02 · Should · R2*
As a Super Administrator, I want interactive users and integration keys rate-limited at the reverse proxy, so that a runaway script or misbehaving integration can't degrade the system for everyone else.
- Given a user exceeds 120 requests/minute (default, configurable), when the next request arrives, then it's refused with a 429, standard rate-limit headers, and a `Retry-After` value.
- Given the limit resets, when the window passes, then requests succeed normally again with no manual intervention needed.

**US-PLAT-09 — Ship breaking API changes as a new, coexisting version**
*SRS §4.8, NFR-MAINT-05 · Should · R2/R3*
As an IT/Infrastructure Team member maintaining an integration against IAMS's API, I want a breaking change to ship as a new major version running alongside the old one for at least 6 months, with deprecation headers on the retiring version, so that my integration doesn't break without warning.
- Given a breaking v2 API change ships, when deployed, then v1 continues serving unchanged for at least 6 months.
- Given a request hits a retiring v1 endpoint, when responded to, then it carries RFC 8594 deprecation headers stating the sunset date.

**US-PLAT-10 — Upgrade a deployment via image tag change with rollback-by-restore**
*SRS §4.10, NFR-DEPLOY-04 · Must · R1*
As an IT/Infrastructure Team member, I want a version upgrade to be an image-tag change plus automatic migrations, with a documented pre-upgrade backup and a restore-based rollback path, so that upgrading a production deployment isn't a leap of faith.
- Given a new release, when upgraded per the Installation & Operations Guide, then the pre-upgrade backup step is documented and required, migrations apply automatically, and the guide states host-sizing and backup-verification steps.
- Given an upgrade fails validation post-migration, when rollback is invoked, then restoring the pre-upgrade backup returns the system to its last-known-good state.

**US-PLAT-11 — Meet WCAG 2.1 AA on every screen**
*SRS §4.9, NFR-UX-04, BR-19 · Must · R1–R2 (incremental per screen)*
As an Employee/Volunteer using assistive technology, I want every IAMS screen to meet WCAG 2.1 AA, so that the system is genuinely usable, not just nominally accessible.
- Given a screen is built or modified, when accessibility-tested, then it passes WCAG 2.1 AA checks (contrast, keyboard navigation, screen-reader labeling) before release.
- Given an accessibility audit runs (US-CMP-04), when findings are logged, then they're tracked to remediation, not just recorded.

**US-PLAT-12 — Standardize loading, empty, error, and permission-denied states**
*SRS §4.9, NFR-UX-07 · Should · R2*
As any user, I want every listing/detail screen to show a consistent loading, empty, error (with trace ID), or permission-denied state, so that I always know what's happening rather than guessing at a blank or frozen screen.
- Given a screen's data fails to load, when the error renders, then it shows a clear message plus the `traceId` for support reference — never a raw stack trace or blank panel.
- Given a background job is still running behind a screen, when viewed, then progress is shown rather than an ambiguous stale state.

**US-PLAT-13 — Install IAMS as a PWA with an offline-capable shell**
*SRS §4.9, NFR-UX-03 · Should · R1*
As an Auditor, I want to install IAMS as a Progressive Web App on my phone, so that it behaves like a native app for the mobile-scanning workflow I use most.
- Given a supported mobile browser, when I choose "Add to Home Screen," then IAMS installs as a PWA with its own icon and launches without browser chrome.
- Given the app shell is cached, when opened with no connectivity, then the shell loads (supporting the offline scan queue) rather than showing a browser offline error.

**US-PLAT-14 — Support the documented browser/device matrix**
*SRS §4.9, NFR-UX-06 · Must · R1*
As a Super Administrator, I want IAMS validated against the latest two stable versions of Chrome/Edge/Firefox/Safari desktop plus current default Android/iOS browsers, so that I can tell staff with confidence which devices will work.
- Given the reference device/browser matrix, when core flows (registration, scanning, approval) are tested, then all pass on every listed combination.
- Given a user is on an unsupported browser, when they access IAMS, then a clear "unsupported browser" notice shows rather than a silently broken experience.

**US-PLAT-15 — Isolate the database and object store behind the reverse proxy**
*SRS §2.5 · Must · R1*
As an IT Security Officer, I want only the reverse proxy (or backend, in proxy-less dev setups) reachable from outside the Docker network, so that the database and object store are never directly exposed to end-user clients.
- Given the production Compose stack, when network configuration is inspected, then `iams-db` and `iams-objectstore` have no published ports reachable outside the internal network.
- Given a client attempts to connect directly to the database port from outside the network, when attempted, then the connection is refused at the network layer, not just the application layer.

**US-PLAT-16 — Gate Swagger/API-docs exposure by environment**
*SRS §2.6 · Must · R1*
As an IT Security Officer, I want Swagger UI and `/v3/api-docs` open in dev/staging but requiring an authenticated Super Administrator session in production, so that our full API surface isn't a public reconnaissance map in production.
- Given a `prod` profile deployment, when Swagger UI is requested without an authenticated Super Admin session, then it's refused.
- Given the same request in `dev`/`staging`, when made, then Swagger UI is openly accessible for development purposes.

---

## Appendix — FR Coverage Cross-Check

Every FR in FRS 2.0 §2 now maps to exactly one user story above. Matching is by ID suffix (e.g., `FR-AUD-19` → `US-AUD-19`) **except** for three documented exceptions:
1. The AUD module's signature clarification is folded into **US-AUD-13** (it re-describes FR-AUD-12, not a new numbered FR).
2. **FR-AUD-18** maps to **US-AUD-24**, not US-AUD-18, because it was discovered missing only after US-AUD-01–23 were already numbered against FR-AUD-01–23 sequentially; appended rather than renumbered to avoid breaking every existing cross-reference to US-AUD-13–23 in this document and in the RTM (see v1.1 changelog above).
3. **FR-SEC-03b** maps to **US-SEC-17**, appended after US-SEC-16 for the same reason (US-SEC-01–16 were already numbered against FR-SEC-01–16 treating "03a" as the only 03-slot).

Platform stories (`US-PLAT-*`) map to SRS sections/NFR IDs rather than FR IDs since the SRS assigns no FR to pure architecture/NFR items.

If a future FR is added to the FRS, add its matching `US-<MODULE>-<NN>` here in the same pass that updates the RTM — and re-run the coverage check this revision used (diff every `FR-<MODULE>-<NN>` range stated in FRS §2 against every FR citation in this document) rather than trusting a prior pass's summary count, since that check is exactly what caught FR-AUD-18 and FR-SEC-03b after RTM v1.1 had already reported full coverage.
