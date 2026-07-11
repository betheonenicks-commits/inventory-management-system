# Acceptance Criteria — R1 Must Have Requirements
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-AC-1.0 | **Status:** For Ratification | **Date:** 2026-07-10
**Fulfills:** FRS 2.0 §4 (one happy-path + one exception-path Given/When/Then per R1 Must Have, required before sprint scheduling). R2 Must Haves receive stubs before their first sprint. Format: `AC-<FR>-H` (happy) / `AC-<FR>-X` (exception). All API behaviors per IAMS-API-1.1; error codes per its §1.9 catalog.

## 1. Asset Management (AST)

**AC-AST-01-H** Given an Inventory Manager with org scope covering Room 204, When she registers an asset with a valid category and required fields, Then the asset is created with a system-generated unique number `AST-YYYY-NNNNNN`, version 0, and an `asset_created` history entry, and the response includes barcode/QR values.
**AC-AST-01-X** Given a required custom field for the chosen category is missing, When registration is submitted, Then the API returns 400 `VALIDATION_FAILED` naming the field, and no asset or history row is created.
**AC-AST-02-H** Given a registered asset, When the label is requested as PNG/SVG/PDF, Then it renders Code 128 + QR (ECC ≥ M) encoding the asset number at the configured label size.
**AC-AST-02-X** Given the label printer is unreachable, When registration completes, Then the asset is still created and the label remains downloadable later (printing never blocks registration).
**AC-AST-03-H** Given an Administrator, When she creates a category with a custom-field schema, Then new assets in that category validate against it.
**AC-AST-03-X** Given a category referenced by ≥1 asset, When deletion is attempted, Then 409 with a dependent-count message and no deletion.
**AC-AST-05-H** Given an asset, When a 2MB JPEG invoice is uploaded, Then metadata (type, size, SHA-256) is stored, the binary lands in the object store only, and the file streams back through the backend to an authorized user.
**AC-AST-05-X** Given a 30MB file or an .exe, When upload is attempted, Then 413 `FILE_TOO_LARGE` or 415 `FILE_TYPE_NOT_ALLOWED` and nothing is written to the object store.
**AC-AST-06-H** Given a category defining a required date custom field, When an asset is created with a valid date, Then the value persists in `custom_fields` and is searchable in advanced filters.
**AC-AST-06-X** Given a text value in the date field, When submitted, Then 400 `VALIDATION_FAILED` citing `customFields.<name>`.
**AC-AST-07-H** Given an asset In Storage, When status changes to In Use with the read `version`, Then the change persists, version increments, and history records old→new.
**AC-AST-07-X** Given a stale `version`, When the update is submitted, Then 409 `OPTIMISTIC_LOCK_CONFLICT` with current state; no change is applied.
**AC-AST-08-H** Given warranty end 2029-07-01, When saved, Then the asset appears in the warranty-expiry report within the configured lookahead of that date.
**AC-AST-08-X** Given warranty end before warranty start, When submitted, Then 400 `VALIDATION_FAILED`.
**AC-AST-09-H** Given purchase details (vendor, date, cost, PO ref), When saved, Then they persist and appear on the asset detail and register report.
**AC-AST-09-X** Given a negative purchase cost, When submitted, Then 400 with `errors[{field:"purchaseCost"}]`.
**AC-AST-10-H** Given any change to status/location/assignment/condition, When committed, Then an append-only history row records field, old/new, actor, timestamp.
**AC-AST-10-X** Given any actor including Super Administrator, When a history row update/delete is attempted via API, Then 405 with a pointer to the correct state-transition endpoint; the row is unchanged.
**AC-AST-11-H** Given an asset moved Room 204 → Room 310, When the move commits, Then the movement log shows from/to nodes, actor, timestamp, and the movement report includes it for the date range.
**AC-AST-11-X** Given a move to a nonexistent org node, When submitted, Then 400/404 and no movement entry.
**AC-AST-13-H** Given a valid 3,000-row import template, When dry run then commit is executed, Then valid rows create assets with generated numbers and one reconciliation report records created/rejected counts. (Full flow: AC-MIG-03.)
**AC-AST-13-X** Given 60 invalid rows, When committed after dry run, Then only valid rows commit and all 60 rejections appear in the reconciliation report — nothing silently dropped.
**AC-AST-14-H** Given insurance details on an asset, When saved, Then insurer/policy/coverage/expiry persist and surface in the insurance-expiry report lookahead. *(Should Have — included as it ships R1.)*
**AC-AST-14-X** Given expiry in the past, When saved, Then the record saves with a visible "expired" indicator (not an error).

## 2. Organization Management (ORG)

**AC-ORG-01-H** Given a Super Admin, When she builds Campus→Building→Floor→Room, Then the tree persists, renders in tree view, and descendant queries return all nested assets.
**AC-ORG-01-X** Given a node with assets or users scoped to it, When deletion is attempted, Then 409 `ORG_NODE_HAS_DEPENDENTS` with the dependent list.
**AC-ORG-02-H** Given hierarchy level "Campus", When relabeled to "Parish", Then all UI/reports show "Parish" and existing data is unaffected.
**AC-ORG-02-X** Given a blank label, When submitted, Then 400 `VALIDATION_FAILED`.
**AC-ORG-03-H** Given a Department with a cost center and no physical node, When created, Then assets and persons can reference it independently of location.
**AC-ORG-03-X** Given a department with assigned assets, When deletion is attempted, Then 409 with dependents.
**AC-ORG-04-H** Given a volunteer who never logs in, When a Person record is created and an asset assigned to them, Then the employee-asset report lists it — no user account required.
**AC-ORG-04-X** Given a Person with assigned assets, When erasure is requested, Then it is blocked per FR-LIF-14/FR-USR-08 with the blocking-asset list.
**AC-ORG-05-H** Given a Department Head scoped to Science, When she lists assets, Then only Science-scoped assets return (implicit org-scope filter).
**AC-ORG-05-X** Given the same user, When she fetches an out-of-scope asset by ID, Then 403/404 and a `security_activity_log` permission-denied entry.

## 3. Users, RBAC & SoD (USR)

**AC-USR-01-H** Given a Super Admin, When she creates a user with role Auditor and scope Building B, Then the user authenticates and `GET /auth/me` shows role, scope, and computed permissions.
**AC-USR-01-X** Given an Administrator, When she attempts to grant Super Administrator, Then 403 (security-sensitive roles are Super Admin-only to assign).
**AC-USR-03-H** Given a Viewer, When she opens the asset register, Then read succeeds and write controls are absent.
**AC-USR-03-X** Given a Viewer crafting a direct `POST /assets` API call, When submitted with her token, Then 403 server-side (UI hiding is not the control).
**AC-USR-04-H** Given a role scoped to Campus North, When any list/search runs, Then results are limited to Campus North descendants.
**AC-USR-04-X** Given scope removal from a user mid-session, When their next request arrives, Then the new scope is enforced (no stale-permission caching beyond token refresh).
**AC-USR-05-H** Given a System Operator, When she opens backup/LDAP/system-health settings, Then access succeeds.
**AC-USR-05-X** Given the same System Operator, When she requests asset valuations or person PII, Then 403.
**AC-USR-06-H** Given user A submitted a transfer, When user B (authorized approver) approves, Then approval succeeds and records approver identity.
**AC-USR-06-X** Given user A submitted it, When A attempts approval with no active waiver, Then 403 `SOD_SELF_APPROVAL_BLOCKED`.
**AC-USR-08-H** Given a user with zero assigned assets, When deactivated, Then status becomes Deactivated, sessions/refresh tokens are revoked, and the event is logged.
**AC-USR-08-X** Given 3 assigned assets, When deactivation is attempted, Then 409 `USER_HAS_OUTSTANDING_ASSIGNMENTS` listing the 3 with resolution actions; after reassignment/return, the same call succeeds.
**AC-USR-09-H** Given a single-admin parish, When a waiver for AUDIT_APPROVAL is recorded with a resolvable IT Security Officer sign-off, Then it activates and the FR-AUD-22 reroute path engages on submission conflicts.
**AC-USR-09-X** Given `securityOfficerSignOff: true` naming a user without the IT Security Officer role (or the requester themself), When submitted, Then 400 `VALIDATION_FAILED` — sign-off cannot be self-asserted.

## 4. Security (SEC)

**AC-SEC-01-H** Given valid credentials, When login succeeds, Then a JWT access token + revocable refresh token issue; refresh exchanges succeed until logout revokes them.
**AC-SEC-01-X** Given a revoked refresh token (post-logout-all), When exchange is attempted, Then 401 and a logged event.
**AC-SEC-02-H** Given LDAP configured, When an LDAP user logs in via the same login endpoint, Then the backend delegates transparently and a local Super Admin fallback still works.
**AC-SEC-02-X** Given the LDAP server is down, When an LDAP user attempts login, Then a clear auth-source-unavailable error (not a generic 500) and local-account login is unaffected.
**AC-SEC-03a-H** Given a new Administrator account, When first login completes, Then TOTP enrollment is forced before any other action.
**AC-SEC-03a-X** Given an Admin without completed MFA enrollment, When she calls any protected endpoint with a partial session, Then 401/403 until enrollment completes.
**AC-SEC-04-H** Given any login, permission change, export, or audit submission, When it occurs, Then an append-only `security_activity_log` row records actor, type, IP, timestamp.
**AC-SEC-04-X** Given a failed login with an unknown username, When logged, Then the row records the event with null user_id and no username-existence leak in the API response.
**AC-SEC-05-H** Given a policy of min-12 + complexity, When a compliant password is set, Then it is accepted and stored hashed (bcrypt/Argon2).
**AC-SEC-05-X** Given "password1", When submitted, Then 400 citing the specific unmet rules.
**AC-SEC-06-H** Given a 30-minute timeout, When a session idles 31 minutes, Then the next request requires re-authentication; an in-progress audit's offline queue survives (NFR-UX-07).
**AC-SEC-06-X** Given a step-up-required action (permission change), When invoked with a fresh session but no step-up assertion, Then the step-up challenge is demanded first.
**AC-SEC-08-H** Given person PII at rest, When storage is inspected, Then values are encrypted per SRS NFR-SEC-03 and all endpoints serve over TLS.
**AC-SEC-08-X** Given a plain-HTTP request in a TLS-configured production deployment, When received at the proxy, Then it is redirected/refused.
**AC-SEC-09-H** Given 5 consecutive failed logins, When the 5th fails, Then the account locks for the cool-down, the lockout is logged, and admin/self-service unlock works.
**AC-SEC-09-X** Given a locked account, When correct credentials are supplied during cool-down, Then login is still refused with a lockout message.
**AC-SEC-10-H** Given a departed person with no holds, When a Compliance Officer runs erasure, Then PII fields anonymize, the pseudonym_ref persists in history, and an export of their data was available beforehand.
**AC-SEC-10-X** Given an active legal hold on a linked audit, When anonymization is attempted, Then 423 `LEGAL_HOLD_ACTIVE` and nothing changes.
**AC-SEC-11-H** Given an Administrator, When she filters the Security & Access Log by user+date+type, Then matching entries return, paginated and exportable (FR-RPT-14).
**AC-SEC-11-X** Given a Viewer, When she requests the log, Then 403.
**AC-SEC-14-H** Given an `INTEGRATION_SVC` credential scoped `INT_ACCOUNTING_READ`, When it calls the depreciation export, Then data returns; When it calls any other endpoint, Then 403.
**AC-SEC-14-X** Given an attempt to assign `INTEGRATION_SVC` to a human user, When submitted, Then 400/403 — the role is non-assignable to humans.
**AC-SEC-15-H** Given an integration configured with credentials, When the row is inspected, Then only a secrets-manager reference exists — no plaintext anywhere in DB, Compose files, or logs.
**AC-SEC-15-X** Given a config payload embedding a plaintext secret where a reference is required, When submitted, Then 400 `VALIDATION_FAILED`.
**AC-SEC-16-H** Given a Super Admin invoking break-glass with a reason, When activated, Then elevated access lasts ≤4h, the IT Security Officer + one Administrator are notified immediately, and every action in the window is flagged in the log.
**AC-SEC-16-X** Given no reason supplied, When invocation is attempted, Then 400; Given the window expires, Then elevation ends automatically and the event remains flagged until reviewed.

## 5. Migration (MIG)

**AC-MIG-01-H** Given the ASSET template download, When Priya fills 3,000 rows and uploads in dry-run mode, Then a Job returns 202 and completes with totals (valid/error) without writing any business data.
**AC-MIG-01-X** Given an unknown column or wrong file type, When uploaded, Then the job fails fast with a clear file-level error.
**AC-MIG-03-H** Given a completed dry run with 2,940 valid rows, When commit is called with an Idempotency-Key, Then 2,940 assets are created, the reconciliation report shows 2,940/0/60, and re-sending the same commit replays the cached response without duplicating.
**AC-MIG-03-X** Given the browser closes mid-commit, When Priya returns, Then the job continued server-side and its status/reconciliation are retrievable.
**AC-MIG-04-H** Given three import runs, When the history is listed, Then who/when/counts/outcome show for each, visible to Super Admin/Admin/IT Security Officer.
**AC-MIG-04-X** Given an Inventory Manager (unauthorized), When she requests import history, Then 403.

## 6. Compliance (CMP)

**AC-CMP-01-H** Given a retention policy `security_activity_log = 7 years, delete`, When the policy engine runs, Then only rows older than 7 years and not under hold are purged, and the purge itself is logged.
**AC-CMP-01-X** Given a policy shorter than the BRD §5.4 floor, When saved, Then 400 citing the floor.
**AC-CMP-02-H** Given a departed volunteer flagged eligible, When the Compliance Officer approves, Then anonymization completes and historical audit findings still reference the stable pseudonym.
**AC-CMP-02-X** Given assets still assigned, When approval is attempted, Then blocked per FR-USR-08/FR-LIF-14 with the asset list.
**AC-CMP-05-H** Given no enabled outbound integrations, When the data-residency view loads, Then it confirms all stores on-premises with zero flags.
**AC-CMP-05-X** Given ACCOUNTING_EXPORT enabled, When the view loads, Then that flow is flagged with its compliance-review record.
**AC-CMP-06-H** Given a legal hold on audit Q2-2026, When retention purge or anonymization touches it, Then both are blocked (423) until the hold is lifted with reason.
**AC-CMP-06-X** Given a hold-lift by a non-Compliance-Officer/non-Super-Admin, When attempted, Then 403.

## 7. Search & Scanning (SRC, SCN)

**AC-SRC-01-H** Given "Latitude" matches assets and a vendor, When global search runs, Then grouped, scope-filtered results return ≤500ms p95 at 100k assets.
**AC-SRC-01-X** Given a term with zero matches, When searched, Then an explicit empty state (not an error).
**AC-SRC-02-H** Given a scanned code `AST-2026-004821`, When looked up, Then the single matching asset resolves ≤1s.
**AC-SRC-02-X** Given an unrecognized code, When looked up, Then a not-found response with a "register this asset?" affordance for authorized roles.
**AC-SRC-03-H** Given filters category=IT + status=In Use + Building B + date range, When advanced search posts the filter tree, Then the paginated envelope returns matching rows only.
**AC-SRC-03-X** Given an unsupported sort field, When requested, Then 400 `INVALID_SORT_FIELD`.
**AC-SCN-01/02-H** Given a USB or Bluetooth scanner in HID mode focused on the scan field, When a label is scanned, Then the value resolves identically to typed input — no driver install.
**AC-SCN-01/02-X** Given a scan of a non-IAMS barcode, When resolved, Then UNRECOGNIZED handling (no crash, value queued for review in audit context).
**AC-SCN-03-H** Given Chrome on Android and Safari on iOS (per NFR-UX-06), When camera scanning starts, Then decode happens client-side and resolves ≤1s p95 on the reference devices.
**AC-SCN-03-X** Given camera permission denied, When scanning starts, Then a clear permission prompt/fallback to manual entry — never a blank screen.
**AC-SCN-04-H** Given asset X already scanned in this session, When scanned again, Then DUPLICATE is signaled with the original scan reference and no double-count.
**AC-SCN-04-X** Given the same scan replayed with the same Idempotency-Key (offline sync), When received, Then the cached response replays with `Idempotency-Replayed: true` and counts are unchanged.
**AC-SCN-05-H** Given normal on-prem network, When 100 sequential scans run, Then p95 scan-to-display ≤1s.
**AC-SCN-05-X** Given server unreachable mid-scan, When scanning continues, Then scans queue locally with visible queue depth (FR-AUD-19 behavior, testable in R1 lookup UI as offline indication).
**AC-SCN-07-H** Given the symbology endpoint, When queried, Then Code128 + QR(ECC≥M) and configured label sizes return; labels print correctly on 50×25mm stock via OS print.
**AC-SCN-07-X** Given an unsupported label size request, When submitted, Then 400 listing supported sizes.

## 8. Auth Deep Links & Identity (NTF-10, INT-03)

**AC-NTF-10-H** Given an approval email deep link and an expired session, When the approver clicks it, Then login (honoring SSO/MFA) intervenes and lands them on the exact approval afterward.
**AC-NTF-10-X** Given a deep link to a resource outside the user's scope, When followed post-login, Then 403 with a safe landing page — never a broken route.
**AC-INT-03-H** Given a configured OIDC provider, When a user completes the SSO flow, Then an IAMS session issues mapped to their account; LDAP-delegated login also works through the standard login endpoint.
**AC-INT-03-X** Given an invalid/expired IdP assertion, When the callback fires, Then the session is refused and the failure logged — no partial session.

## 9. Test-Readiness Notes (TST-04/05 closure aids)
Integration test environments required before R1 hardening: an LDAP test container (e.g., OpenLDAP with seeded users), an SMTP catcher (MailHog-class), reference mobile devices per NFR-UX-06, at least one USB HID scanner, and printed label sheets from AC-SCN-07 stock sizes. UAT entry: all R1 AC-H scenarios green in staging + migration rehearsal complete (FR-MIG-05). UAT exit: zero open Critical defects, all AC-X negative paths verified, security sign-off including FR-SEC-13 pen test, and restore drill (NFR-DEPLOY-04) executed once.
