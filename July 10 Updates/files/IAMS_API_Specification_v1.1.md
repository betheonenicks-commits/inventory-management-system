**REST API / Interface Specification**

**Inventory Audit Management System (IAMS)**

*Implementation-Ready Contract Between the React Frontend, External Integrations, and the Spring Boot Backend*

Document ID: IAMS-API-1.1 | Version: 1.1 | Status: For Ratification

Date: 2026-07-10

Related Documents: IAMS-BRD-2.0 (Business Requirements), IAMS-FRS-2.0 (Functional Requirements), IAMS-SRS-2.0 (Software/Architecture Requirements), IAMS-DD-1.1 (Data Dictionary), IAMS-PUC-1.1 (Personas & Use Cases), IAMS-AC-1.0 (Acceptance Criteria)


# Changelog — v1.1 (2026-07-10)

This revision reconciles the API contract to the ratified consolidated baseline (BRD 2.0 / FRS 2.0 / SRS 2.0 / DD 1.1). No endpoint paths, payloads, or behaviors changed — only requirement references and document pointers:

1. **FR-ID renumbering applied per FRS 2.0 Appendix B:** insurance FR-AST-13→**FR-AST-14**; vehicle FR-AST-14→**FR-AST-15**; depreciation FR-AST-15→**FR-AST-16**; offboarding FR-USR-05→**FR-USR-08**; SoD waivers FR-USR-07→**FR-USR-09**; webhook signing FR-SEC-10→**FR-INT-06**. FR-AST-13 (bulk import), FR-USR-05 (System Operator), FR-USR-07 (flat role model), FR-SEC-10 (data-subject erasure) retain their FRS meanings and are not cited by the renumbered rows.
2. **SRS section citations** map to SRS 2.0 per its Appendix A (e.g., draft "SRS 6.7/6.8" → NFR-SEC-10 / §2.5).
3. **Open Items 1–3 resolved by the baseline:** role modeling confirmed (9 defaults + 2 system-provided custom roles + INTEGRATION_SVC — FR-USR-01); SSO/LDAP in R1 under SEC confirmed (FR-INT-03, BRD §8.2); `return-to-inventory` in R1 confirmed (FR-USR-08). Open Item 4 (commit-with-errors) confirmed as designed (FR-MIG-03). Items 5–7 remain flagged for load-test/backlog review.

# Document Control

## Purpose

This document is the implementation contract for IAMS's REST API surface. It is written so that (a) a backend engineer can scaffold Spring Boot `@RestController` classes directly from Section 3's endpoint tables, (b) a frontend engineer can build API client code against Section 4's concrete JSON shapes without waiting on a running backend, and (c) an integration partner can build against Section 7 without backend source access. It restates no business rationale from the BRD/FRS/SRS beyond what is needed to justify an API design decision; for *why* a rule exists, see the cited FR-ID/NFR-ID.

## How to Read the Release Tags

Every endpoint is tagged **R1**, **R2**, or **R3**, per the release mapping in BRD Section 8.2 and the FR-level appendix in FRS Section 5. R1 = MVP/Core, R2 = Audit & Lifecycle (the differentiator), R3 = Migration completion, Integration & Scale. Where this document had to make a release-sequencing call the source documents left ambiguous, it is called out explicitly in a **Judgment Call** callout box at the point of decision, and summarized again in Section 10 (Open Items).

## Judgment Call — Global Design Decisions Not Fully Specified Upstream

The following cross-cutting decisions are this document's own resolution of gaps in the BRD/FRS/SRS, made using standard REST/OWASP ASVS practice. They are not re-litigated at each usage site:

1. All resource identifiers (`{id}` path parameters) are server-generated UUIDv4 — non-enumerable, safe to expose in URLs, consistent across all 16 modules. Human-readable business keys (asset number, PO number) are separate fields in the resource body and are also accepted as lookup values via the Search module (Section 3.11), never as the primary path identifier.
2. Roles referenced throughout this document use these codes: `SUPER_ADMIN`, `ADMIN`, `INVENTORY_MANAGER`, `AUDITOR`, `READONLY_AUDITOR`, `DEPT_HEAD`, `EMPLOYEE` (covers the combined Employee/Volunteer persona), `VIEWER`. FR-USR-01 defines these eight as the default role set. The BRD/PUC additionally describe an **IT Security Officer** and a **Data Protection/Compliance Officer** as distinct stakeholders with distinct capabilities (BRD Section 2, SRS Section 6) that do not map cleanly onto any of the eight defaults. This document models them as two additional **system-provided custom roles** — `IT_SECURITY_OFFICER` and `COMPLIANCE_OFFICER` — built on the FR-USR-02 custom-role mechanism, shipped pre-defined out of the box rather than requiring an Administrator to construct them from scratch. A ninth non-human role, `INTEGRATION_SVC`, is used exclusively by external-system service accounts (FR-SEC-14) and is never assignable to a human user.
3. Org-scope filtering (FR-USR-04): unless an endpoint's table row says otherwise, **every list/detail/search endpoint implicitly filters results to the caller's assigned organizational-hierarchy scope**, intersected with their role's permissions. This is not repeated per row. `EMPLOYEE` additionally defaults to seeing only entities assigned to or created by themselves, regardless of org scope, except where a row states otherwise.
4. `DELETE` is reserved for resources that carry no audit-trail/history obligation (saved searches, webhook subscriptions, draft purchase requests, unused custom-role definitions). Business entities with append-only history requirements (assets, audits, findings, lifecycle events, transactions) are never hard-deleted through the API — corrections are state-transition actions (e.g., `POST /assets/{id}/void-registration`, restricted to `SUPER_ADMIN` within a short post-creation grace window) or new linked records, per SRS Section 4.1 and FR-AUD-18.
5. Bulk/long-running operations use a single, consistent job-polling pattern (Section 8) rather than WebSockets or Server-Sent Events, to keep operational complexity low for the target IT-generalist persona (BRD Section 11.2, NFR-DEPLOY-03) and to avoid a second connection protocol through the reverse proxy.

# 1. API Conventions

## 1.1 Base Path and Versioning

- All endpoints are served under **`/api/v1`**. There is no unversioned path; the version segment is mandatory from day one, per SRS Section 2.2 ("API Layer... versioned `/api/v1/...`").
- The version is a **URI path segment**, not a header or query parameter, so that the version is visible in logs, browser network tabs, and reverse-proxy routing rules without inspecting headers — the simplest option for the on-premises, small-IT-team operating model this system targets.
- A future breaking change is shipped as `/api/v2`, running side-by-side with `/api/v1` for a deprecation window (Section 9.3), never as an in-place breaking change to `/api/v1`.

## 1.2 Resource Naming Rules

- Resource collection paths are **plural nouns**, **kebab-case** for multi-word resources: `/assets`, `/inventory-items`, `/purchase-orders`, `/sod-waivers`, `/org-nodes`.
- Ownership/containment is expressed via nested paths only one level deep: `/assets/{id}/attachments`, `/audits/{id}/findings`. Where a nested resource is also independently addressable and frequently fetched on its own (e.g., a single attachment, a single finding), a top-level shortcut path is also provided (`/attachments/{attachmentId}`) so a client holding only that ID doesn't need the parent ID to fetch it.
- Non-CRUD business operations (approve, close, transfer, stock-out, deactivate) are modeled as **`POST /{resource}/{id}/{verb}`** action endpoints, not shoehorned into `PUT`/`PATCH` on the resource. This is a deliberate, industry-standard deviation from pure resource-oriented REST (the same pattern used by the Stripe and GitHub APIs): these operations are not idempotent field replacements, they are business transactions with their own authorization rules, side effects, and — for several of them — Separation-of-Duties checks (FR-USR-06) that a generic `PATCH` cannot express cleanly.

## 1.3 HTTP Verb Semantics

| Verb | Use | Idempotent? | Notes |
|---|---|---|---|
| `GET` | Read a resource or collection | Yes | Never mutates state; safe to cache/retry freely. |
| `POST` | Create a resource (`201` + `Location` header), or invoke a named action/command endpoint (`200`/`202`) | No (unless `Idempotency-Key` supplied — Section 1.7) | The most heavily used verb in this API given the action-endpoint pattern above. |
| `PUT` | Full replacement of a **singleton configuration resource** only (e.g., `PUT /compliance/retention-policy`, `PUT /security/session-policy`) | Yes | Not used for business entities that carry a `version` — those use `PATCH`. |
| `PATCH` | Partial update of a versioned business entity | No | IAMS uses **partial JSON merge PATCH** (send only changed fields plus the `version` you last read), not RFC 6902 JSON Patch — the frontend is a purpose-built SPA, not a generic HTTP client, so the added complexity of a patch-operations DSL isn't justified. See Section 5 for the concurrency contract this implies. |
| `DELETE` | Hard-remove a resource with no audit-trail obligation | Yes | See Judgment Call #4 above. Attempting `DELETE` on an entity that must remain append-only returns `405 Method Not Allowed` with a problem-detail body pointing to the correct state-transition endpoint. |

## 1.4 Standard Success Envelope

- **Single-resource responses** (`GET /assets/{id}`, `POST /assets`, `PATCH /assets/{id}`, and most action endpoints) return the resource JSON **directly at the top level** — no wrapper object. HTTP status code carries the outcome (`200`, `201`, `202`, `204`). Mutating responses always include the entity's current `version` and `updatedAt` so the client can immediately use them for its next `PATCH`.
- **Collection responses** (`GET /assets`, `GET /audits`, etc.) are **always wrapped** in a pagination envelope (Section 1.5) — never a bare JSON array — because NFR-SCALE-03 mandates pagination metadata be present on every list endpoint, and a bare array has nowhere to carry it.
- **Action endpoints that return no meaningful body** (e.g., `POST /notifications/{id}/read`) return `204 No Content`.
- **Action endpoints that kick off background work** (Section 8) return `202 Accepted` with a `Job` resource body and a `Location` header pointing at `/jobs/{jobId}`.

## 1.5 Pagination Envelope

Mandatory on every list endpoint (NFR-SCALE-03 — "no unpaginated collection endpoints"). Query parameters:

| Param | Default | Notes |
|---|---|---|
| `page` | `0` | Zero-indexed. |
| `size` | `20` | Max `200`. Requests above the max are **clamped** to `200` (not rejected with `400`) — availability over strictness for a UI-driven client base. |
| `sort` | endpoint-specific, documented per resource | Repeatable: `sort=createdAt,desc&sort=assetNumber,asc`. |

Response shape:

```json
{
  "data": [ { "...": "resource" } ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 1345,
    "totalPages": 68
  },
  "sort": ["createdAt,desc"]
}
```

## 1.6 Filtering and Sorting Query Conventions

- Simple filters are flat query parameters matched to resource fields: `GET /assets?status=IN_USE&categoryId=...&purchaseDateFrom=2026-01-01&purchaseDateTo=2026-06-30&orgNodeId=...`. A free-text `q` parameter is supported on every list endpoint for a quick-filter match against the resource's primary searchable fields (name, asset number, serial number, etc.).
- **Advanced combinable filtering** (FR-SRC-03) that exceeds comfortable query-string complexity is exposed as **`POST /{resource}/search`** accepting a structured filter-tree JSON body and returning the identical pagination envelope as the `GET` list endpoint. This is a deliberate, accepted "POST-as-query" exception to "GET for reads" — advanced ad hoc search doesn't need to be cacheable or bookmarkable, and a filter tree with nested AND/OR groups is impractical to encode reliably in a query string.
- Sort fields are documented per endpoint in Section 3; unsupported sort fields return `400` with `errorCode: INVALID_SORT_FIELD`.

## 1.7 Idempotency-Key Header

Any `POST` that mutates state and might plausibly be retried by the client (network timeout, offline-queue replay, double-tap on a mobile scan screen) accepts an **`Idempotency-Key`** request header — a client-generated UUIDv4, unique per logical operation. Required (request rejected with `400 IDEMPOTENCY_KEY_REQUIRED`) on:

- `POST /audits/{id}/scans` and `POST /audits/{id}/scans/offline-sync` — critical per FR-AUD-19: a replayed offline scan must not double-count.
- `POST /inventory-items/{id}/stock-in`, `/stock-out`, `/adjust`, `/transfer` — atomic inventory mutations (NFR-CONC-02) that a client might retry after an ambiguous network failure.
- `POST /migrations/imports/{jobId}/commit` — a long-running commit a client might resubmit after a timeout.
- `POST /integrations/webhooks/{id}/test`.

Server behavior: the `(actor, endpoint, Idempotency-Key)` tuple is stored with its resulting response for **48 hours**. A repeat request with the same key returns the **original cached response** unchanged (same status code, same body) with an added `Idempotency-Replayed: true` response header — the mutation is not re-executed. A repeat request with the same key but a materially different body returns `409` with `errorCode: IDEMPOTENCY_KEY_REUSE_MISMATCH`. Optional on all other mutating `POST` endpoints; recommended for any bulk operation.

## 1.8 Rate-Limit Response Contract

Enforced at the reverse-proxy / API-gateway layer per NFR-API-01, per authenticated user and per integration API key. Every response carries:

```
X-RateLimit-Limit: 120
X-RateLimit-Remaining: 47
X-RateLimit-Reset: 1752054000
```

On violation, per NFR-API-02:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 30
Content-Type: application/problem+json
```
```json
{
  "type": "https://iams.internal/problems/rate-limit-exceeded",
  "title": "Rate limit exceeded",
  "status": 429,
  "detail": "Request rate for this account exceeded 120 requests/minute.",
  "instance": "/api/v1/assets",
  "errorCode": "RATE_LIMITED"
}
```

## 1.9 Standard Error Format — RFC 7807/9457 `application/problem+json`

**Decision: every non-2xx response body is `application/problem+json`**, per RFC 7807 (obsoleted/superseded by RFC 9457, same wire shape). Justification: (1) Spring Boot 6/Spring Framework 6 (which underlies Spring Boot 3, SRS Section 3) ships a native `ProblemDetail` type and `ErrorResponse` support, so this is close to "free" on the backend rather than a hand-rolled convention; (2) it's an IETF standard with a registered media type, so any HTTP tooling, API gateway, or third-party integration client already knows how to parse it, which matters for FR-INT external integrators who are not IAMS's own frontend team; (3) it's extensible via custom members without breaking the standard shape — used here to carry `errorCode` and endpoint-specific structured payloads (blocking-asset lists, conflict diffs) that a generic `detail` string cannot express machine-readably.

Standard fields (all responses) plus IAMS extensions:

```json
{
  "type": "https://iams.internal/problems/{problem-slug}",
  "title": "Short, human-readable summary of the problem type",
  "status": 409,
  "detail": "Human-readable explanation specific to this occurrence",
  "instance": "/api/v1/audits/9911.../close",
  "errorCode": "AUDIT_SCOPE_CHANGE_UNRESOLVED",
  "traceId": "a1b2c3d4-...",
  "timestamp": "2026-07-09T10:22:31.442Z"
}
```

- `type` resolves to a static, human-readable explanation page served by the backend itself (on-premises, no external dependency) — never a `4xx`/`5xx` dead link.
- `errorCode` is the field client code should branch on (stable, uppercase snake_case); `type`/`title`/`detail` may change wording across releases, `errorCode` values are contractually stable within a major API version.
- `traceId` matches the correlation ID in the structured backend logs (NFR-OBS-03), so a Super Administrator can hand a `traceId` to IT/support and it's directly greppable.
- Validation failures (`400`) add an `errors` array: `[{ "field": "purchaseCost", "message": "must be >= 0" }]`.
- Selected `errorCode` catalog used throughout this document (non-exhaustive — each module section may define additional codes): `VALIDATION_FAILED`, `OPTIMISTIC_LOCK_CONFLICT`, `SOD_SELF_APPROVAL_BLOCKED`, `SOD_APPROVER_CONFLICT_NO_WAIVER`, `AUDIT_SCOPE_CHANGE_UNRESOLVED`, `INSUFFICIENT_STOCK`, `DUPLICATE_SCAN`, `USER_HAS_OUTSTANDING_ASSIGNMENTS`, `RECONCILIATION_NOT_APPLICABLE`, `INTEGRATION_DISABLED`, `FILE_TYPE_NOT_ALLOWED`, `FILE_TOO_LARGE`, `LEGAL_HOLD_ACTIVE`, `IDEMPOTENCY_KEY_REQUIRED`, `IDEMPOTENCY_KEY_REUSE_MISMATCH`, `RATE_LIMITED`, `ORG_NODE_HAS_DEPENDENTS`.

# 2. Authentication and Session Endpoints

Per FR-SEC-01/02, SRS Section 6.1: local JWT auth is always available as the Super Administrator fallback; LDAP/AD delegated auth and SAML2/OIDC SSO are configurable alternatives. **Judgment Call**: authentication mechanisms themselves (local, LDAP, SSO) are core to R1 (BR-07 is an R1-delivered business requirement — a system nobody can log into isn't a viable MVP), even though the broader External Integrations *governance* module (FR-INT-03's enable/disable framework, Section 3.14) ships in R3 alongside the rest of module INT. Identity-provider *configuration* endpoints therefore live under `/security/identity-providers` (module SEC, R1), while the generic per-integration enable/disable catalog (module INT, R3) governs everything else external-facing (accounting export, HR/SIS sync, webhooks).

| Method | Path | Purpose | Auth Required | Release | FR-ID |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/login` | Local or LDAP-delegated login (transparent to the client — backend routes to the configured provider); body `{username, password}` | No | R1 | FR-SEC-01, FR-SEC-02 |
| POST | `/api/v1/auth/refresh` | Exchange a valid refresh token for a new access token; body `{refreshToken}` or via `HttpOnly` cookie | Refresh token | R1 | FR-SEC-01 |
| POST | `/api/v1/auth/logout` | Revoke the current refresh token server-side (SRS 6.1 "revocable, stored server-side") | Access token | R1 | FR-SEC-01 |
| POST | `/api/v1/auth/logout-all` | Revoke all refresh tokens for the user (suspected compromise) | Access token | R1 | FR-SEC-04 |
| GET | `/api/v1/auth/sso/{provider}/authorize` | Redirect-initiate a SAML2/OIDC login flow for a configured provider | No | R1 | FR-SEC-02, FR-INT-03 |
| GET/POST | `/api/v1/auth/sso/{provider}/callback` | IdP redirects/posts back here (OIDC callback = `GET`, SAML ACS = `POST`); exchanges assertion for an IAMS session | No (validated via signed assertion) | R1 | FR-SEC-02, FR-INT-03 |
| POST | `/api/v1/auth/mfa/challenge` | Submit a TOTP code to complete login when 2FA is enabled | Partial session token | R2 (Could Have, FR-SEC-03) | FR-SEC-03 |
| GET | `/api/v1/auth/me` | Current user profile, effective roles, org scope, and computed permission set | Access token | R1 | FR-USR-03, FR-USR-04 |
| POST | `/api/v1/auth/password` | Change own password | Access token | R1 | FR-SEC-05 |
| POST | `/api/v1/auth/password/reset-request` | Request a password reset link (local auth only) | No | R1 | FR-SEC-05 |
| POST | `/api/v1/auth/password/reset` | Complete a password reset with a one-time token | No | R1 | FR-SEC-05 |
| POST | `/api/v1/auth/step-up` | Re-authenticate within an active session for a sensitive action (permission change, bulk export, retention/legal-hold change) — returns a short-lived step-up assertion the sensitive endpoint requires | Access token | R1 | NFR-SEC (SRS 6.2), FR-SEC-06 |

`GET /auth/me` response shape:

```json
{
  "userId": "uuid",
  "username": "priya.admin",
  "displayName": "Priya Sharma",
  "roles": ["SUPER_ADMIN"],
  "orgScope": [{ "orgNodeId": "uuid", "nodeType": "ORGANIZATION", "name": "Riverside Diocese" }],
  "permissions": ["ASSET_WRITE", "AUDIT_APPROVE", "USER_MANAGE", "..."],
  "authProvider": "LOCAL",
  "mfaEnabled": false,
  "mustChangePassword": false
}
```

# 3. Full Endpoint Catalog by Module

Unless a row states a different role, write endpoints additionally require the caller's org scope to cover the target entity (Judgment Call #3). All list endpoints follow Section 1.5's pagination envelope; omitted from the "Purpose" column for brevity.

## 3.1 Asset Management (AST)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/assets` | List/filter assets | SUPER_ADMIN, ADMIN, INVENTORY_MANAGER, AUDITOR, READONLY_AUDITOR, DEPT_HEAD, VIEWER | R1 | FR-AST-01, FR-AST-03, FR-AST-07 |
| POST | `/assets/search` | Advanced combinable-filter search | same as above | R1 | FR-AST-01, FR-SRC-03 |
| POST | `/assets` | Register a new asset (generates asset number + barcode/QR) | INVENTORY_MANAGER, ADMIN, SUPER_ADMIN | R1 | FR-AST-01, FR-AST-02, FR-AST-06, FR-AST-09 |
| GET | `/assets/{id}` | Asset detail | broad read roles (as list) + EMPLOYEE if self-assigned | R1 | FR-AST-01 |
| PATCH | `/assets/{id}` | Partial update (optimistic-locked, Section 5) | INVENTORY_MANAGER, ADMIN, SUPER_ADMIN | R1 | FR-AST-06, FR-AST-07, FR-AST-08, FR-AST-09 |
| POST | `/assets/{id}/void-registration` | Void an erroneous registration within a configurable grace window (not a hard delete — status becomes `VOID`, retained in history) | SUPER_ADMIN | R1 | FR-AST-01, SRS 4.1 |
| PATCH | `/assets/{id}/status` | Explicit status transition (In Use/In Storage/Under Repair/etc.) | INVENTORY_MANAGER, ADMIN | R1 | FR-AST-07 |
| GET | `/assets/{id}/history` | Append-only status/location/assignment/condition history, paginated | broad read roles | R1 | FR-AST-10, NFR-PERF-05 |
| GET | `/assets/{id}/movements` | Movement (location-change) log specifically, paginated | broad read roles | R1 | FR-AST-11 |
| POST | `/assets/{id}/attachments` | Upload image/invoice/manual/warranty file (Section 6) | INVENTORY_MANAGER, ADMIN, SUPER_ADMIN | R1 | FR-AST-05 |
| GET | `/assets/{id}/attachments` | List attachment metadata | broad read roles | R1 | FR-AST-05 |
| GET | `/attachments/{attachmentId}` | Stream a single attachment through the backend (Section 6) | broad read roles (checked per parent asset's scope) | R1 | FR-AST-05 |
| DELETE | `/attachments/{attachmentId}` | Remove an attachment | INVENTORY_MANAGER, ADMIN, SUPER_ADMIN | R1 | FR-AST-05 |
| GET | `/assets/{id}/label` | Render the asset's barcode/QR label (`?format=png|svg|pdf`) | broad read roles | R1 | FR-AST-02, FR-SCN-07 |
| POST | `/assets/labels/batch` | Batch label generation for a filtered set — async Job (Section 8) | INVENTORY_MANAGER, ADMIN | R2 | FR-RPT-11, FR-SCN-07 |
| POST | `/assets/{id}/children` | Link a child asset (e.g., laptop + charger) | INVENTORY_MANAGER, ADMIN | R1 | FR-AST-04 |
| GET | `/assets/{id}/children` | List linked child assets | broad read roles | R1 | FR-AST-04 |
| DELETE | `/assets/{id}/children/{childId}` | Unlink | INVENTORY_MANAGER, ADMIN | R1 | FR-AST-04 |
| GET | `/asset-categories` | List categories/custom-field schema | broad read roles | R1 | FR-AST-03, FR-AST-06 |
| POST | `/asset-categories` | Create category | ADMIN, SUPER_ADMIN | R1 | FR-AST-03 |
| PATCH | `/asset-categories/{id}` | Update category / custom field definitions | ADMIN, SUPER_ADMIN | R1 | FR-AST-06 |
| DELETE | `/asset-categories/{id}` | Delete an unused category (`409 ORG_NODE_HAS_DEPENDENTS`-style block if assets reference it) | ADMIN, SUPER_ADMIN | R1 | FR-AST-03 |
| PATCH | `/assets/{id}/insurance` | Set/update dedicated insurance columns (insurer, policy #, coverage, expiry) | INVENTORY_MANAGER, ADMIN | R1 | FR-AST-14 |
| PATCH | `/assets/{id}/vehicle-attributes` | Set/update dedicated Vehicle subtype columns (VIN, registration, odometer, expiry) | INVENTORY_MANAGER, ADMIN | R1 | FR-AST-15 |
| GET | `/assets/{id}/depreciation` | Computed depreciation schedule for one asset | broad read roles | R2 | FR-AST-16 |

**Judgment Call**: FR-AST-01–14 (registration, categorization, attachments, custom fields, history, insurance, vehicle attributes) ship in R1 — they're required to produce the R1 "basic asset register" (BRD 8.2). FR-AST-16 (depreciation calculation) ships in R2 because its only consumer, FR-RPT-09 (Depreciation report), is explicitly R2 per the FRS Section 5 release-mapping appendix; shipping the calculation engine a release early with no report to expose it through has no user value.

## 3.2 Inventory Management (INV) — R2 (BR-06)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/inventory-items` | List/filter inventory items | INVENTORY_MANAGER, ADMIN, SUPER_ADMIN, VIEWER | R2 | FR-INV-01 |
| POST | `/inventory-items` | Create an inventory item definition | INVENTORY_MANAGER, ADMIN | R2 | FR-INV-01 |
| GET | `/inventory-items/{id}` | Detail incl. quantity-on-hand per warehouse | as list | R2 | FR-INV-01 |
| PATCH | `/inventory-items/{id}` | Partial update, optimistic-locked (definition fields only — never quantity) | INVENTORY_MANAGER, ADMIN | R2 | FR-INV-01, FR-INV-04 |
| POST | `/inventory-items/{id}/stock-in` | Atomic Stock In (Section 5) | INVENTORY_MANAGER | R2 | FR-INV-02 |
| POST | `/inventory-items/{id}/stock-out` | Atomic Stock Out | INVENTORY_MANAGER | R2 | FR-INV-02 |
| POST | `/inventory-items/{id}/adjust` | Manual adjustment; mandatory `reason` + `approverId` | INVENTORY_MANAGER (submit), ADMIN (approve) | R2 | FR-INV-05 |
| POST | `/inventory-items/{id}/transfer` | Atomic inter-warehouse transfer (Section 4.7) | INVENTORY_MANAGER | R2 | FR-INV-03, FR-INV-08, NFR-CONC-02 |
| GET | `/inventory-items/{id}/transactions` | Paginated transaction history | as list | R2 | FR-INV-02 |
| POST | `/inventory-items/{id}/batches` | Register a lot/batch with optional expiry date | INVENTORY_MANAGER | R2 | FR-INV-09 |
| GET | `/inventory-items/low-stock` | Items below configured reorder threshold | as list | R2 | FR-INV-04, FR-DSH-04 |
| GET | `/inventory-items/expiring` | Batches approaching expiry (configurable lookahead) | as list | R2 | FR-INV-09 |
| GET | `/inventory-items/{id}/valuation` | Costing-method valuation (weighted-average, multi-currency) | INVENTORY_MANAGER, ADMIN, VIEWER | R2 | FR-INV-06, FR-INV-10 |
| GET | `/warehouses` | List warehouses/storage locations | as list | R2 | FR-INV-03 |
| POST | `/warehouses` | Create warehouse | ADMIN, SUPER_ADMIN | R2 | FR-INV-03 |
| GET/POST | `/warehouses/{id}/bins` | Shelf/bin sub-locations | ADMIN, INVENTORY_MANAGER | R2 | FR-INV-03 |
| GET | `/vendors` | List vendors | as list | R2 | FR-INV-07 |
| POST | `/vendors` | Create vendor | INVENTORY_MANAGER, ADMIN | R2 | FR-INV-07 |
| GET | `/vendors/{id}/purchase-history` | Purchase history linked to this vendor | as list | R2 | FR-INV-07, FR-RPT-06 |

## 3.3 Organization Management (ORG) — R1 (BR-02)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/org-nodes` | List hierarchy (flat with `parentId`, or `?view=tree`) | all authenticated roles (scope-filtered) | R1 | FR-ORG-01, FR-ORG-05 |
| POST | `/org-nodes` | Create a node (campus/building/floor/room/department/etc.) | SUPER_ADMIN, ADMIN | R1 | FR-ORG-01, FR-ORG-05 |
| GET | `/org-nodes/{id}` | Node detail | all authenticated roles | R1 | FR-ORG-01 |
| PATCH | `/org-nodes/{id}` | Rename/relabel/move node | SUPER_ADMIN, ADMIN | R1 | FR-ORG-02 |
| DELETE | `/org-nodes/{id}` | Delete a node; `409 ORG_NODE_HAS_DEPENDENTS` with a dependent-asset list if assets/users are still scoped to it (UC-ORG-01) | SUPER_ADMIN | R1 | FR-ORG-01 |
| GET | `/org-hierarchy-types` | List configurable hierarchy level names (Campus/Building/... or Ministry/Department/...) | SUPER_ADMIN, ADMIN | R1 | FR-ORG-02 |
| PATCH | `/org-hierarchy-types/{levelId}` | Relabel a hierarchy level | SUPER_ADMIN | R1 | FR-ORG-02 |
| GET/POST | `/departments` | Department/cost-center entities independent of physical location | ADMIN, SUPER_ADMIN (write); broad (read) | R1 | FR-ORG-03 |
| GET/POST | `/employees` | Employee/Volunteer records (personal data, FR-CMP tagged) | ADMIN, SUPER_ADMIN (write); scoped (read) | R1 | FR-ORG-04 |
| GET | `/employees/{id}` | Employee detail | ADMIN, SUPER_ADMIN, DEPT_HEAD (own dept), self | R1 | FR-ORG-04 |
| PATCH | `/employees/{id}` | Update employee record | ADMIN, SUPER_ADMIN | R1 | FR-ORG-04 |
| GET/POST | `/room-types` | Specialized location-type variants (Classroom, Laboratory) of the generic Room entity | SUPER_ADMIN, ADMIN | R1 | FR-ORG-06 |

## 3.4 Asset Lifecycle Management (LIF) — R2 (BR-04)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET/POST | `/purchase-requests` | Create/list Purchase Requests | INVENTORY_MANAGER (create), ADMIN (read all) | R2 | FR-LIF-01 |
| POST | `/purchase-requests/{id}/approve` | Approve prior to PO issuance | ADMIN, DEPT_HEAD | R2 | FR-LIF-01 |
| GET/POST | `/purchase-orders` | Create/list Purchase Orders | INVENTORY_MANAGER, ADMIN | R2 | FR-LIF-02 |
| GET | `/purchase-orders/{id}` | PO detail incl. linked received assets | as above + VIEWER | R2 | FR-LIF-02 |
| POST | `/purchase-orders/{id}/receive` | Receiving step: reconcile delivered items against the PO before registration | INVENTORY_MANAGER | R2 | FR-LIF-03 |
| POST | `/assets/{id}/assign` | Assign asset to employee/volunteer/department/room | INVENTORY_MANAGER, ADMIN | R2 | FR-LIF-04 |
| POST | `/assets/{id}/return-to-inventory` | Return an assigned asset to unassigned inventory (used by offboarding, Section 4.5) | INVENTORY_MANAGER, ADMIN | R1 (needed for offboarding, FR-USR-08) | FR-LIF-04, FR-USR-08 |
| POST | `/assets/{id}/transfer` | Initiate a Transfer (creates a pending approval if configured) | INVENTORY_MANAGER, ADMIN, DEPT_HEAD | R2 | FR-LIF-05 |
| GET | `/transfers` | List transfer requests | involved parties + ADMIN | R2 | FR-LIF-05 |
| GET | `/transfers/{id}` | Transfer detail | as above | R2 | FR-LIF-05 |
| POST | `/transfers/{id}/approve` | Approve — server-side blocks self-approval (Section 4.4) | DEPT_HEAD (scoped) | R2 | FR-LIF-05, FR-USR-06 |
| POST | `/transfers/{id}/reject` | Reject with mandatory reason | DEPT_HEAD (scoped) | R2 | FR-LIF-05 |
| POST | `/assets/{id}/repairs` | Log a repair event (vendor, cost, downtime) | INVENTORY_MANAGER | R2 | FR-LIF-06 |
| GET | `/assets/{id}/repairs` | Repair history | broad read roles | R2 | FR-LIF-06 |
| GET/POST | `/maintenance-schedules` | Preventive maintenance schedules | INVENTORY_MANAGER, ADMIN | R2 | FR-LIF-07 |
| POST | `/assets/{id}/maintenance-events` | Log corrective maintenance (fault- or audit-finding-triggered) | INVENTORY_MANAGER | R2 | FR-LIF-08 |
| POST | `/assets/{id}/retire` | Initiate Retirement/Disposal/Donation (reason, method, e-waste compliance flag, recovery value) | INVENTORY_MANAGER | R2 | FR-LIF-09 |
| GET | `/disposals` | List pending/completed disposals | ADMIN, INVENTORY_MANAGER | R2 | FR-LIF-09 |
| POST | `/disposals/{id}/approve` | Administrator approval finalizing status as Disposed; blocked if an open insurance claim is unresolved (`409 OPEN_INSURANCE_CLAIM`) | ADMIN, SUPER_ADMIN | R2 | FR-LIF-09, UC-LIF-02 |

**Judgment Call**: `POST /assets/{id}/return-to-inventory` is pulled forward to R1 even though the rest of module LIF is R2, because FR-USR-08 (Offboarding, R1 per BR-21's R1 scope and the fact user management is core R1) requires *some* way to detach an asset from a deactivated user, and R1 has no full Transfer/Assignment workflow yet. In R1, this endpoint performs the minimal unassign-and-flag-unassigned operation; the richer Transfer/Assignment approval workflow (FR-LIF-04/05) supersedes it in R2.

## 3.5 Audit Management (AUD) — R2 (BR-05, the core differentiator)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/audits` | List/filter audits | AUDITOR, READONLY_AUDITOR, DEPT_HEAD, ADMIN, SUPER_ADMIN, VIEWER | R2 | FR-AUD-01 |
| POST | `/audits` | Create an audit (type, scope, assigned auditor/approver, sampling mode); expected-asset snapshot generated synchronously at creation | AUDITOR, ADMIN, SUPER_ADMIN | R2 | FR-AUD-01, FR-AUD-02, FR-AUD-03 |
| GET | `/audits/{id}` | Audit detail | as list | R2 | FR-AUD-01 |
| PATCH | `/audits/{id}` | Edit before start (optimistic-locked) | AUDITOR (own), ADMIN | R2 | FR-AUD-01 |
| GET | `/audits/{id}/expected-assets` | The scope snapshot fixed at creation, paginated | AUDITOR, DEPT_HEAD, ADMIN | R2 | FR-AUD-03 |
| GET | `/audits/sampling-preview` | Compute sample size/method preview for a proposed scope before creating a sample-based audit | AUDITOR, ADMIN | R2 | FR-AUD-20 |
| POST | `/audits/{id}/start` | Transition DRAFT → IN_PROGRESS | AUDITOR (assigned) | R2 | FR-AUD-01 |
| POST | `/audits/{id}/scans` | **Continuous Scan Mode scan-resolution endpoint** — resolves one scan, <1s p95 (Section 4.2) | AUDITOR (assigned) | R2 | FR-AUD-04, FR-AUD-05, FR-SCN-05, NFR-PERF-02 |
| POST | `/audits/{id}/scans/batch` | Batch-scan multiple co-located assets in one call | AUDITOR (assigned) | R2 | FR-AUD-06 |
| POST | `/audits/{id}/scans/offline-sync` | Replay a client-side offline scan queue; each item individually idempotent-keyed | AUDITOR (assigned) | R2 | FR-AUD-19, NFR-AVAIL-05 |
| GET | `/audits/{id}/progress` | Real-time Expected vs. Verified counts | AUDITOR, DEPT_HEAD, ADMIN | R2 | FR-AUD-07 |
| POST | `/audits/{id}/findings` | Record a finding: condition assessment, photo evidence, remarks | AUDITOR (assigned) | R2 | FR-AUD-09, FR-AUD-10, FR-AUD-11 |
| GET | `/audits/{id}/findings` | List findings | AUDITOR, DEPT_HEAD, ADMIN, READONLY_AUDITOR | R2 | FR-AUD-09 |
| POST | `/audits/{id}/findings/{findingId}/corrections` | Immutable correction — a new linked record, never an in-place edit (FR-AUD-18) | AUDITOR (assigned), ADMIN | R2 | FR-AUD-18 |
| POST | `/audits/{id}/submit` | Digitally sign and submit for approval; computes/reroutes the approver (Section 4.4) | AUDITOR (assigned) | R2 | FR-AUD-12, FR-AUD-13, FR-AUD-22 |
| POST | `/audits/{id}/approve` | Approve — server-side self-approval block (Section 4.4) | DEPT_HEAD (scoped), or rerouted alternate/SUPER_ADMIN | R2 | FR-AUD-13, FR-AUD-22, FR-USR-06 |
| POST | `/audits/{id}/request-clarification` | Pause finalization, request auditor clarification without discarding submitted data | DEPT_HEAD (scoped) | R2 | UC-AUD-03 |
| POST | `/audits/{id}/close` | Finalize; classifies unverified expected assets Missing; **blocked** by undispositioned scope changes (Section 4.3) | DEPT_HEAD, or rerouted approver, SUPER_ADMIN | R2 | FR-AUD-08, FR-AUD-23 |
| GET | `/audits/{id}/certificate` | Signed Audit Completion Certificate (PDF) | AUDITOR, DEPT_HEAD, ADMIN, READONLY_AUDITOR, VIEWER | R2 | FR-AUD-14 |
| GET | `/audits/{id}/exceptions` | Exception Report (Missing + Damaged) | as certificate | R2 | FR-AUD-15 |
| GET | `/audits/{id}/scope-changes` | List assets flagged "Scope Changed During Audit" | AUDITOR, DEPT_HEAD, ADMIN | R2 | FR-AUD-23 |
| POST | `/audits/{id}/scope-changes/{assetId}/disposition` | Resolve a flagged scope change (Section 4.3) | AUDITOR (assigned), DEPT_HEAD | R2 | FR-AUD-23 |
| POST | `/assets/{id}/reconcile` | Reconcile a previously-Missing asset found outside an active audit (Section 4.6) | AUDITOR, INVENTORY_MANAGER | R2 | FR-AUD-21 |
| GET | `/audit-analytics` | Cross-cycle trend comparison (missing-asset rate, etc.), flags non-like-for-like comparisons | ADMIN, DEPT_HEAD, VIEWER, READONLY_AUDITOR | R2 | FR-AUD-17 |

## 3.6 Barcode/QR/RFID Scanning (SCN) — R1 core lookup, R2 audit-integrated modes

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/scan-lookup?code={value}` | Resolve a scanned/typed code to an asset or inventory item outside an audit context (general-purpose, <1s target) | broad read roles | R1 | FR-SCN-01–05, FR-SRC-02 |
| GET | `/labels/symbology` | Returns the supported symbology (Code128, QR min ECC level M) and label-size configuration | broad read roles | R1 | FR-SCN-07 |

Continuous Scan Mode's dedicated in-audit endpoint is `POST /audits/{id}/scans` (Section 3.5) — kept under module AUD rather than SCN because its response shape must carry live audit-progress counters (FR-AUD-07) that a generic scan-resolution endpoint has no business returning.

## 3.7 Reporting (RPT)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/reports/asset-register` | Filterable Asset Register (on-screen preview, paginated) | broad read roles | R1 | FR-RPT-01 |
| POST | `/reports/asset-register/export` | Async export (PDF/Excel/CSV) — Job pattern (Section 8) | broad read roles | R1 | FR-RPT-01, FR-RPT-12 |
| GET | `/reports/employee-assets/{employeeId}` | Employee Asset List | ADMIN, DEPT_HEAD (scoped), self | R1 | FR-RPT-03 |
| POST | `/reports/employee-assets/{employeeId}/export` | Async export | as above | R1 | FR-RPT-03, FR-RPT-12 |
| GET | `/reports/department-inventory` | Department/Room/Building Inventory reports (`?level=department\|room\|building`) | broad read roles | R2 | FR-RPT-02 |
| GET | `/reports/missing-assets` , `/lost-assets`, `/damaged-assets` | Audit-finding-sourced exception reports | broad read roles | R2 | FR-RPT-04 |
| GET | `/reports/warranty-expiry`, `/amc-expiry`, `/insurance-expiry` | Expiry reports, configurable lookahead window (`?lookaheadDays=`) | broad read roles | R2 | FR-RPT-05 |
| GET | `/reports/purchase-history`, `/reports/vendors` | Purchase/vendor reports | INVENTORY_MANAGER, ADMIN, VIEWER | R2 | FR-RPT-06 |
| GET | `/reports/asset-movement` | Location/assignment changes over a date range | broad read roles | R2 | FR-RPT-07 |
| GET | `/reports/audit-compliance`, `/reports/audit-summary` | Cross-cycle audit reports incl. reconciliation outcomes | ADMIN, DEPT_HEAD, VIEWER, READONLY_AUDITOR | R2 | FR-RPT-08 |
| GET | `/reports/depreciation` | Depreciation report, multi-currency (Section 4.8) | ADMIN, VIEWER, SUPER_ADMIN | R2 | FR-RPT-09, FR-INV-10 |
| GET | `/reports/maintenance-history` | Maintenance history report | INVENTORY_MANAGER, ADMIN, VIEWER | R2 | FR-RPT-10 |
| POST | `/reports/labels/batch` | Batch label printing for a filtered asset set — Job | INVENTORY_MANAGER, ADMIN | R2 | FR-RPT-11 |
| GET/POST/DELETE | `/report-schedules` | Configure recurring report generation/delivery | ADMIN, VIEWER (own schedules) | R2 | FR-RPT-13 |

All `/reports/*/export` and equivalent large-output endpoints return `202` + a `Job` (Section 8) rather than streaming synchronously, per NFR-PERF-04.

## 3.8 Dashboard (DSH) — R2 (BR-08 full)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/dashboard/summary` | Total asset count + breakdowns (category/department/building/campus/status) | all authenticated (scope-filtered) | R2 | FR-DSH-01 |
| GET | `/dashboard/audits` | Live completion % for active audits | AUDITOR, DEPT_HEAD, ADMIN, VIEWER | R2 | FR-DSH-02 |
| GET | `/dashboard/expirations` | Upcoming warranty/AMC/insurance/maintenance-due items | INVENTORY_MANAGER, ADMIN, VIEWER | R2 | FR-DSH-03 |
| GET | `/dashboard/inventory-alerts` | Low-stock and expiry-approaching alerts | INVENTORY_MANAGER, ADMIN | R2 | FR-DSH-04 |
| GET | `/dashboard/activity-feed` | Recent activity feed | all authenticated (scope-filtered) | R2 | FR-DSH-05 |
| GET | `/dashboard/audit-calendar` | Calendar view of scheduled/active audits | AUDITOR, DEPT_HEAD, ADMIN | R2 | FR-DSH-05 |
| GET | `/dashboard/kpis` | Configured KPI widgets with data | all authenticated (scope-filtered) | R2 | FR-DSH-06 |
| PUT | `/dashboard/kpis/config` | Configure which KPIs display | ADMIN, SUPER_ADMIN | R2 | FR-DSH-06 |

Dashboard scope-filtering per FR-DSH-07 is the same global org-scope rule as Judgment Call #3 — not a separate mechanism.

## 3.9 User Management & RBAC (USR) — R1 (BR-21 SoD is explicitly R1 per FRS Section 5)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/users` | List users | ADMIN, SUPER_ADMIN | R1 | FR-USR-01 |
| POST | `/users` | Create a user account | ADMIN, SUPER_ADMIN | R1 | FR-USR-01 |
| GET | `/users/{id}` | User detail | ADMIN, SUPER_ADMIN, self | R1 | FR-USR-01 |
| PATCH | `/users/{id}` | Update profile fields (optimistic-locked) | ADMIN, SUPER_ADMIN, self (limited fields) | R1 | FR-USR-01 |
| POST | `/users/{id}/deactivate` | Offboard: blocked while assets remain assigned (Section 4.5) | ADMIN, SUPER_ADMIN | R1 | FR-USR-08 |
| POST | `/users/{id}/reactivate` | Reactivate a deactivated account | ADMIN, SUPER_ADMIN | R1 | FR-USR-01 |
| GET | `/roles` | List roles (default + custom) | ADMIN, SUPER_ADMIN | R1 | FR-USR-01, FR-USR-02 |
| POST | `/roles` | Create a custom role with a configurable permission set | ADMIN, SUPER_ADMIN | R1 | FR-USR-02 |
| PATCH/DELETE | `/roles/{id}` | Update/delete a custom role (default roles are read-only) | SUPER_ADMIN | R1 | FR-USR-02 |
| PUT | `/users/{id}/roles` | Assign role(s) to a user | ADMIN (standard roles), SUPER_ADMIN (security-sensitive roles) | R1 | FR-USR-01, PUC Section 2 |
| PUT | `/users/{id}/org-scope` | Assign organizational-hierarchy scope | ADMIN, SUPER_ADMIN | R1 | FR-USR-04 |
| GET | `/sod-waivers` | List Separation-of-Duties waivers | ADMIN, SUPER_ADMIN, IT_SECURITY_OFFICER | R1 | FR-USR-09 |
| POST | `/sod-waivers` | Record a waiver (Section 4.4) | SUPER_ADMIN | R1 | FR-USR-09 |
| POST | `/sod-waivers/{id}/revoke` | Revoke an active waiver | SUPER_ADMIN | R1 | FR-USR-09 |

FR-USR-06 (self-approval block) has no dedicated endpoint — it is a server-side check embedded in every approval-style action endpoint (`transfers/{id}/approve`, `audits/{id}/approve`, `disposals/{id}/approve`, `inventory-items/{id}/adjust` approval, etc.), documented once in Section 4.4 rather than per row.

## 3.10 Notifications (NTF) — R2 (BR-09, Should Have)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/notifications` | In-app notifications, read/unread state | self | R2 | FR-NTF-03 |
| PATCH | `/notifications/{id}/read` | Mark one as read | self | R2 | FR-NTF-03 |
| POST | `/notifications/mark-all-read` | Bulk mark read | self | R2 | FR-NTF-03 |
| GET | `/notification-preferences` | Per-event-type preference (Email/In-App/Both/None) | self | R2 | FR-NTF-05 |
| PUT | `/notification-preferences` | Update preferences; mandatory types rejected with `423`-style locked-field indicator, not silently ignored | self | R2 | FR-NTF-05 |
| GET/PUT | `/notification-types` | Admin config of which types are mandatory | ADMIN, SUPER_ADMIN | R2 | FR-NTF-05 |

## 3.11 Search (SRC)

**Judgment Call**: module SRC is not listed in the FRS Section 5 release-mapping appendix and does not map to a single numbered BR. Basic lookup (FR-SRC-01/02/03) is pulled into **R1** because it is a practical prerequisite for the R1 asset register to be usable at all (a register nobody can search through spreadsheet-replaces-nothing); saved searches (FR-SRC-04, "Should Have") ship in **R2** as a convenience layered on top.

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/search?q={term}` | Global search across assets, employees, vendors, purchase orders | broad read roles | R1 | FR-SRC-01 |
| GET | `/search/lookup?code={value}` | Direct lookup by barcode/QR/serial/asset number | broad read roles | R1 | FR-SRC-02 |
| POST | `/search/advanced` | Combinable-filter search (category/status/location/department/date range) | broad read roles | R1 | FR-SRC-03 |
| GET/POST/DELETE | `/saved-searches` | Save/re-run/delete frequent searches | self | R2 | FR-SRC-04 |

## 3.12 Security (SEC) — R1 core

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/security/activity-log` | Immutable log of user activity, login history, admin actions — filterable by user/date/type | SUPER_ADMIN, IT_SECURITY_OFFICER | R1 | FR-SEC-04 |
| GET | `/security/login-history` | Login-attempt-specific subset (UC-SEC-01) | SUPER_ADMIN, IT_SECURITY_OFFICER | R1 | FR-SEC-04 |
| GET/PUT | `/security/password-policy` | Configure min length/complexity/expiry/reuse rules | SUPER_ADMIN | R1 | FR-SEC-05 |
| GET/PUT | `/security/session-policy` | Configure session timeout, step-up-required action list | SUPER_ADMIN | R1 | FR-SEC-06 |
| GET/PUT | `/security/identity-providers/ldap` | LDAP/AD connection config | SUPER_ADMIN | R1 | FR-SEC-02 |
| GET/PUT | `/security/identity-providers/sso` | SAML2/OIDC provider config | SUPER_ADMIN | R1 | FR-SEC-02, FR-INT-03 |
| POST | `/users/{id}/mfa/enroll` | Begin TOTP enrollment | self | R2 (Could Have) | FR-SEC-03 |
| POST | `/users/{id}/mfa/verify` | Confirm enrollment with a TOTP code | self | R2 | FR-SEC-03 |
| DELETE | `/users/{id}/mfa` | Disable 2FA | self, SUPER_ADMIN | R2 | FR-SEC-03 |
| GET/PUT | `/security/ip-restrictions` | Optional IP allow-list configuration | SUPER_ADMIN | R2 (Could Have) | FR-SEC-07 |
| GET | `/security/dependency-scan-status` | Latest CI dependency-scan summary (informational) | SUPER_ADMIN, IT_SECURITY_OFFICER | R2 | FR-SEC-12, FR-SEC-13 |

## 3.13 Data Migration & Bulk Import/Export (MIG) — R1 for import/dry-run/log, R3 for export

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/migrations/templates/{entityType}` | Download the CSV/Excel import template for `ASSET`, `EMPLOYEE`, or `VENDOR` | SUPER_ADMIN, ADMIN | R1 | FR-MIG-01 |
| POST | `/migrations/imports` | Upload a file in `DRY_RUN` mode — returns a Job (Section 8, Section 4.3) | SUPER_ADMIN, ADMIN | R1 | FR-MIG-01, FR-MIG-03 |
| GET | `/migrations/imports/{jobId}` | Poll job status/summary | SUPER_ADMIN, ADMIN | R1 | FR-MIG-03 |
| GET | `/migrations/imports/{jobId}/report` | Per-row validation report (valid/rejected + reasons), paginated | SUPER_ADMIN, ADMIN | R1 | FR-MIG-01, FR-MIG-03 |
| POST | `/migrations/imports/{jobId}/commit` | Commit a previously dry-run'd import — new Job; `Idempotency-Key` required | SUPER_ADMIN, ADMIN | R1 | FR-MIG-03 |
| GET | `/migrations/imports/{jobId}/reconciliation` | Reconciliation report (created/updated/rejected counts, generated identifiers) | SUPER_ADMIN, ADMIN | R1 | FR-MIG-03 |
| GET | `/migrations/imports` | History of all import runs (who, when, row counts, outcome) | SUPER_ADMIN, ADMIN, IT_SECURITY_OFFICER | R1 | FR-MIG-04 |
| POST | `/migrations/exports` | Bulk export of asset/employee/vendor records, re-import-compatible format — Job | SUPER_ADMIN, ADMIN | R3 | FR-MIG-02 |
| GET | `/migrations/exports/{jobId}` | Poll export job / download link | SUPER_ADMIN, ADMIN | R3 | FR-MIG-02 |

## 3.14 External Integrations (INT) — R3 (BR-16), except identity-provider config (R1, Section 3.12/2)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/integrations` | List all integration points and enabled/disabled status | SUPER_ADMIN, ADMIN, COMPLIANCE_OFFICER | R3 | FR-INT-05 |
| PUT | `/integrations/{key}` | Enable/disable/configure one integration; `complianceReviewedBy` required when enabling an outbound-data-flow integration (BRD 6.5) | SUPER_ADMIN | R3 | FR-INT-05, FR-SEC-09 |
| GET | `/integrations/{key}/health` | Last successful call, error rate, credential expiry | SUPER_ADMIN, ADMIN | R3 | FR-INT-05 |
| GET | `/integrations/accounting-export/depreciation` | Read-scoped depreciation export (Section 7) | INTEGRATION_SVC, VIEWER, SUPER_ADMIN | R3 | FR-INT-01 |
| GET | `/integrations/accounting-export/asset-valuation` | Read-scoped valuation export | INTEGRATION_SVC, VIEWER, SUPER_ADMIN | R3 | FR-INT-01 |
| POST | `/integrations/hr-sync/run` | Trigger an HR/SIS roster sync | SUPER_ADMIN, ADMIN | R3 | FR-INT-02 |
| GET | `/integrations/hr-sync/status` | Last sync result | SUPER_ADMIN, ADMIN | R3 | FR-INT-02 |
| GET/POST | `/integrations/webhooks` | List/register outbound webhook subscriptions (allow-listed URL only) | SUPER_ADMIN | R3 | FR-INT-04, FR-INT-06 |
| DELETE | `/integrations/webhooks/{id}` | Remove a webhook subscription | SUPER_ADMIN | R3 | FR-INT-04 |
| GET | `/integrations/webhooks/{id}/deliveries` | Delivery attempt log incl. retries | SUPER_ADMIN, IT_SECURITY_OFFICER | R3 | FR-INT-04, FR-SEC-04 |
| POST | `/integrations/webhooks/{id}/test` | Send a test payload; `Idempotency-Key` required | SUPER_ADMIN | R3 | FR-INT-04 |

## 3.15 Compliance & Data Privacy (CMP) — R1 (BR-17/BR-18 are R1)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET/PUT | `/compliance/retention-policy` | Per-entity-type retention configuration | COMPLIANCE_OFFICER, SUPER_ADMIN | R1 | FR-CMP-01 |
| GET | `/compliance/anonymization-candidates` | Records flagged eligible for anonymization/deletion at policy expiry | COMPLIANCE_OFFICER, SUPER_ADMIN | R1 | FR-CMP-01 |
| POST | `/compliance/anonymize/{entityType}/{id}` | Anonymize a departed employee/volunteer/student record; blocked (`423 LEGAL_HOLD_ACTIVE`) if any referencing record has an active legal hold | COMPLIANCE_OFFICER | R1 | FR-CMP-02 |
| GET/PUT | `/compliance/privacy-notice` | Configured basis/notice text per personal-data field | COMPLIANCE_OFFICER, SUPER_ADMIN | R1 | FR-CMP-03 |
| GET | `/compliance/data-residency-status` | Confirms all data stores are on-premises; flags any enabled external-data-flow integration | SUPER_ADMIN, COMPLIANCE_OFFICER, IT_SECURITY_OFFICER | R1 | FR-CMP-05 |
| GET/POST | `/compliance/legal-holds` | List/create a legal-hold flag on an audit or asset record | COMPLIANCE_OFFICER, SUPER_ADMIN | R1 | FR-CMP-06 |
| DELETE | `/compliance/legal-holds/{id}` | Lift a hold | COMPLIANCE_OFFICER, SUPER_ADMIN | R1 | FR-CMP-06 |
| GET | `/compliance/wcag-audit-status` | Latest accessibility-audit record (informational) | SUPER_ADMIN, COMPLIANCE_OFFICER | R1 | FR-CMP-04 |

## 3.16 Product Analytics (ANL) — R2 (BR-20)

| Method | Path | Purpose | Required Role(s) | Release | FR-ID |
|---|---|---|---|---|---|
| GET | `/analytics/usage-report` | Feature adoption by role, module usage frequency | ADMIN, SUPER_ADMIN | R2 | FR-ANL-03 |
| POST | `/feedback` | Submit free-text + category feedback | all authenticated roles | R2 | FR-ANL-04 |
| GET | `/feedback` | Admin view of submitted feedback | ADMIN, SUPER_ADMIN (configured recipient) | R2 | FR-ANL-04 |

Usage-metric *capture* (FR-ANL-01/02) is a server-side interceptor concern, not a client-called endpoint — there is deliberately no `POST /analytics/events` client API, since exposing one would create exactly the kind of ad hoc external data-egress surface FR-ANL-02 ("no metrics transmitted outside the deployment") and BO-5 are designed to prevent.

# 4. Detailed Request/Response Schemas

## 4.1 Asset Registration (FR-AST-01/02/05/06)

`POST /api/v1/assets`

```json
{
  "categoryId": "b2f6a1e0-1111-4a2e-9c3d-000000000001",
  "name": "Dell Latitude 5440 Laptop",
  "manufacturer": "Dell",
  "modelNumber": "Latitude 5440",
  "serialNumber": "DL5440-88213",
  "vendorId": "9b1c...-vendor",
  "purchaseOrderReference": "PO-2026-0417",
  "purchase": {
    "amount": 1299.00,
    "currency": "USD",
    "fxRateToReportingCurrency": 1.0,
    "fxRateAsOfDate": "2026-07-01",
    "purchaseDate": "2026-07-01"
  },
  "orgNodeId": "7a2e...-room-204",
  "status": "IN_STORAGE",
  "customFields": {
    "legacyAssetTag": "OLD-4471",
    "procurementBudgetLine": "IT-CAPEX-2026"
  },
  "parentAssetId": null,
  "warranty": { "startDate": "2026-07-01", "endDate": "2029-07-01" }
}
```

Response `201 Created`, header `Location: /api/v1/assets/{id}`:

```json
{
  "id": "c4d9e2f1-....",
  "assetNumber": "AST-2026-004821",
  "version": 0,
  "name": "Dell Latitude 5440 Laptop",
  "categoryId": "b2f6a1e0-...",
  "status": "IN_STORAGE",
  "barcode": { "symbology": "CODE128", "value": "AST-2026-004821" },
  "qrCode": { "value": "AST-2026-004821", "errorCorrectionLevel": "M", "labelUrl": "/api/v1/assets/c4d9e2f1-..../label?format=png" },
  "orgNodeId": "7a2e...-room-204",
  "purchase": { "amount": 1299.00, "currency": "USD", "reportingCurrencyAmount": 1299.00 },
  "customFields": { "legacyAssetTag": "OLD-4471", "procurementBudgetLine": "IT-CAPEX-2026" },
  "createdBy": "elena.inv-mgr",
  "createdAt": "2026-07-09T09:00:11.204Z",
  "updatedAt": "2026-07-09T09:00:11.204Z"
}
```

- If a required custom field for `categoryId` is missing: `400` with `errorCode: VALIDATION_FAILED` and `errors: [{ "field": "customFields.warrantyCardNumber", "message": "required for category IT Equipment" }]` (UC-AST-01 alternate flow).
- Attachments are uploaded in a **separate** subsequent call (`POST /assets/{id}/attachments`, multipart) rather than embedded in this JSON body — keeps the registration payload uniform JSON and lets a slow/failed upload be retried independently without re-submitting the whole asset (matches UC-AST-01's alternate flow: "if the label printer is unreachable, registration still completes").

## 4.2 Audit Creation + Continuous Scan Mode Scan-Resolution + Audit Closure with Scope-Change Blocking (FR-AUD-03/04/05/23)

**Create:** `POST /api/v1/audits`

```json
{
  "type": "DEPARTMENT",
  "name": "Q3 2026 Science Department Audit",
  "scope": { "orgNodeIds": ["room-uuid-1", "room-uuid-2"], "categoryIds": [] },
  "auditorId": "devon-auditor-uuid",
  "departmentHeadId": "father-thomas-uuid",
  "scheduledStart": "2026-07-15",
  "scheduledEnd": "2026-07-16",
  "samplingMode": "FULL"
}
```

Response `201`: includes `id`, `status: "DRAFT"`, `expectedAssetCount: 812` (computed synchronously from the scope at creation time per FR-AUD-03 — the snapshot, not a live query, is what the audit is measured against for its whole lifecycle).

**Continuous Scan Mode — the sub-1-second endpoint:** `POST /api/v1/audits/{id}/scans`

Required header: `Idempotency-Key: <client-generated-scan-uuid>` (so a replayed scan — offline sync, double-tap, retried timeout — never double-counts, FR-AUD-19).

```json
{
  "scanValue": "AST-2026-004821",
  "scannedAt": "2026-07-15T09:14:02.331Z",
  "deviceId": "devon-android-phone-01",
  "method": "CAMERA"
}
```

Response `200` (target p95 < 1s per FR-SCN-05/NFR-PERF-02):

```json
{
  "scanId": "f1a2....",
  "resolution": "VERIFIED",
  "asset": {
    "id": "c4d9e2f1-....",
    "assetNumber": "AST-2026-004821",
    "name": "Dell Latitude 5440 Laptop",
    "expectedOrgNodeId": "room-uuid-1"
  },
  "auditProgress": { "expectedCount": 812, "verifiedCount": 233, "percentComplete": 28.7 }
}
```

`resolution` is one of `VERIFIED` | `DUPLICATE` | `UNRECOGNIZED` | `OUT_OF_SCOPE`. On `DUPLICATE`, `duplicateOfScanId` is populated and the count is not incremented (FR-SCN-04). On `UNRECOGNIZED`, `asset` is `null` and the raw value is queued to a review list rather than silently discarded (UC-AUD-01 alternate flow). A retried request with the same `Idempotency-Key` returns the identical cached response with header `Idempotency-Replayed: true` and does **not** increment `verifiedCount` again.

**Closure — blocked by an unresolved scope change:** `POST /api/v1/audits/{id}/close`

Success: `200`, `status: "CLOSED"`, `certificateUrl` populated, unverified expected assets classified `MISSING` (FR-AUD-08).

Blocked: `409 Conflict`

```json
{
  "type": "https://iams.internal/problems/audit-scope-change-unresolved",
  "title": "Audit cannot be closed while scope-changed assets are undispositioned",
  "status": 409,
  "detail": "3 assets flagged 'Scope Changed During Audit' require disposition before this audit can be finalized.",
  "instance": "/api/v1/audits/aa11.../close",
  "errorCode": "AUDIT_SCOPE_CHANGE_UNRESOLVED",
  "blockingAssets": [
    {
      "assetId": "c4d9e2f1-....",
      "assetNumber": "AST-2026-004821",
      "reason": "TRANSFERRED",
      "triggeringEvent": { "type": "TRANSFER", "eventId": "tr-99...", "occurredAt": "2026-07-15T11:02:00Z", "fromOrgNodeId": "room-uuid-1", "toOrgNodeId": "room-uuid-9" },
      "requiredAction": "POST /api/v1/audits/aa11.../scope-changes/c4d9e2f1-..../disposition"
    }
  ]
}
```

Disposition: `POST /api/v1/audits/{id}/scope-changes/{assetId}/disposition`

```json
{ "decision": "CONFIRM_VERIFIED_AT_NEW_LOCATION", "note": "Confirmed present at new location per FR-LIF-05 transfer record tr-99..." }
```

`decision` ∈ `CONFIRM_VERIFIED_AT_NEW_LOCATION` | `EXCLUDE_FROM_SCOPE` | `ACCEPT_AS_EXCEPTION` (mirrors FR-AUD-23's three explicit options). Once every blocking asset is dispositioned, `close` succeeds.

## 4.3 Bulk Import Dry-Run + Commit (FR-MIG-01/03)

`POST /api/v1/migrations/imports` (multipart: `file`, `entityType=ASSET`, `mode=DRY_RUN`)

Response `202 Accepted`, `Location: /api/v1/migrations/imports/{jobId}`:

```json
{ "jobId": "job-771...", "status": "PENDING", "mode": "DRY_RUN", "entityType": "ASSET", "fileName": "assets_2026.xlsx", "submittedBy": "priya.superadmin", "submittedAt": "2026-07-09T09:30:00Z" }
```

`GET /api/v1/migrations/imports/{jobId}` (poll):

```json
{ "jobId": "job-771...", "status": "COMPLETED", "mode": "DRY_RUN", "progressPercent": 100, "summary": { "totalRows": 3000, "validRows": 2940, "errorRows": 60 } }
```

`GET /api/v1/migrations/imports/{jobId}/report` (paginated):

```json
{
  "data": [
    { "rowNumber": 42, "status": "ERROR", "errors": [{ "field": "category", "message": "Unknown category 'Lab Equip' — did you mean 'Lab Equipment'?" }], "rawData": { "assetName": "Microscope", "category": "Lab Equip" } },
    { "rowNumber": 43, "status": "VALID", "preview": { "assetName": "Bunsen Burner", "category": "Lab Equipment", "willCreateAssetNumber": "AST-2026-004822" } }
  ],
  "page": { "number": 0, "size": 50, "totalElements": 3000, "totalPages": 60 }
}
```

`POST /api/v1/migrations/imports/{jobId}/commit` — header `Idempotency-Key` required. Commits only rows that were `VALID` in the referenced dry run; `ERROR` rows remain rejected and appear in the reconciliation report so nothing is silently dropped. Response `202` with a new commit `Job`.

`GET /api/v1/migrations/imports/{jobId}/reconciliation`:

```json
{
  "jobId": "job-771...",
  "entityType": "ASSET",
  "createdCount": 2940,
  "updatedCount": 0,
  "rejectedCount": 60,
  "reportDownloadUrl": "/api/v1/migrations/imports/job-771.../reconciliation/export"
}
```

**Judgment Call**: commit is permitted any time after a completed dry run, even with outstanding error rows — FR-MIG-03 describes the dry run/reconciliation pattern but does not mandate zero errors before commit, and UC-MIG-01's flow (re-run dry run until zero errors) is Priya's chosen *workflow*, not a system-enforced gate. Enforcing zero-errors-to-commit would block an organization from getting 2,940 good rows in while it fixes 60 bad ones, which works against the "R1 must let an organization stop using spreadsheets" goal (BRD 8.1).

## 4.4 SoD Waiver Creation + Self-Approval-Blocked Error (FR-USR-06/07)

`POST /api/v1/sod-waivers`

```json
{
  "scope": { "orgNodeId": "campus-uuid-north", "actionTypes": ["AUDIT_APPROVAL", "TRANSFER_APPROVAL"] },
  "reason": "Single-site non-profit with one Administrator; BRD Section 2.1 control accepted as not applicable.",
  "securityOfficerSignOff": true,
  "signedOffBy": "reyes.security-officer",
  "effectiveFrom": "2026-07-09",
  "effectiveTo": null
}
```

Response `201`: `{ "id": "waiver-uuid", "status": "ACTIVE", ... }`. `securityOfficerSignOff: true` without a resolvable `signedOffBy` holding role `IT_SECURITY_OFFICER` is rejected `400 VALIDATION_FAILED` — the sign-off flag can't be self-asserted (BRD Section 6.5).

**Self-approval blocked** — e.g., `POST /api/v1/audits/{id}/approve` called by the same identity that submitted the audit, no active waiver covering `AUDIT_APPROVAL` for that scope:

```json
{
  "type": "https://iams.internal/problems/sod-self-approval-blocked",
  "title": "Self-approval is not permitted",
  "status": 403,
  "detail": "User 8f3c... submitted this audit and cannot also approve it. An active Separation-of-Duties Waiver covering AUDIT_APPROVAL for this org scope would allow an alternate approval path.",
  "instance": "/api/v1/audits/9911.../approve",
  "errorCode": "SOD_SELF_APPROVAL_BLOCKED",
  "actionType": "AUDIT_APPROVAL",
  "conflictingUserId": "8f3c....",
  "waiverEndpoint": "/api/v1/sod-waivers"
}
```

**Reroute when Auditor == Department Head under an active waiver (FR-AUD-22):** the reroute decision is made at `POST /api/v1/audits/{id}/submit`, not at `approve` time — the system knows at submission whether the nominal approver conflicts with the submitter.

```json
{ "auditId": "9911....", "status": "PENDING_APPROVAL", "routedApproverId": "priya.superadmin", "routingReason": "SOD_WAIVER_REROUTE", "waiverId": "waiver-uuid" }
```

If the same identity conflict exists with **no** active waiver, `submit` itself is blocked (`409`, `errorCode: SOD_APPROVER_CONFLICT_NO_WAIVER`), instructing the caller to either assign a different Department Head to the scope or record a waiver — the audit is never silently stuck unapprovable, and it is never silently self-approved.

## 4.5 Offboarding with Blocking Asset List (FR-USR-08, UC-USR-01)

`POST /api/v1/users/{id}/deactivate`

Blocked: `409 Conflict`

```json
{
  "type": "https://iams.internal/problems/user-has-outstanding-assignments",
  "title": "Cannot deactivate user with outstanding asset assignments",
  "status": 409,
  "detail": "3 assets are currently assigned to this user and must be reassigned or returned to inventory before deactivation.",
  "instance": "/api/v1/users/sam-employee-uuid/deactivate",
  "errorCode": "USER_HAS_OUTSTANDING_ASSIGNMENTS",
  "blockingAssets": [
    { "assetId": "a1...", "assetNumber": "AST-2025-000101", "name": "Dell Latitude 5440", "assignedSince": "2025-01-10" },
    { "assetId": "a2...", "assetNumber": "AST-2025-000144", "name": "iPhone 14", "assignedSince": "2025-03-02" },
    { "assetId": "a3...", "assetNumber": "AST-2024-000980", "name": "Vehicle Key Fob #12", "assignedSince": "2024-11-20" }
  ],
  "resolutionActions": [
    "POST /api/v1/assets/{assetId}/assign — reassign to another holder",
    "POST /api/v1/assets/{assetId}/return-to-inventory — return, awaiting reissue"
  ]
}
```

Once every blocking asset is resolved, the identical `POST /users/{id}/deactivate` call succeeds:

```json
{ "id": "sam-employee-uuid", "status": "DEACTIVATED", "deactivatedAt": "2026-07-09T14:02:00Z", "deactivatedBy": "marcus.admin" }
```

## 4.6 Reconciliation of a Previously-Missing Asset (FR-AUD-21)

`POST /api/v1/assets/{id}/reconcile`

```json
{
  "foundAt": "2026-07-09T10:22:00Z",
  "actualOrgNodeId": "building-c-storage-closet-uuid",
  "conditionAssessment": "GOOD",
  "note": "Found in Building C storage closet during unrelated cleanup.",
  "photoAttachmentIds": []
}
```

Response `200`:

```json
{
  "reconciliationId": "rec-uuid",
  "assetId": "c4d9e2f1-....",
  "newStatus": "IN_STORAGE",
  "linkedOriginalExceptionId": "finding-uuid-from-closed-audit",
  "linkedAuditId": "audit-uuid-q2-2026",
  "recordedBy": "devon.auditor",
  "recordedAt": "2026-07-09T10:25:11Z",
  "auditAnalyticsUpdated": true
}
```

If the target asset's current status is not `MISSING`-sourced-from-a-closed-audit, `409` with `errorCode: RECONCILIATION_NOT_APPLICABLE` — this endpoint is specifically the FR-AUD-21 linked-resolution path, not a generic status-edit shortcut (preserves FR-AUD-18 immutability and the BO-2 measurement-integrity rule in BRD 1.3.1 that a reduction only counts if closed via this workflow).

## 4.7 Inventory Stock Transfer (FR-INV-08)

`POST /api/v1/inventory-items/{id}/transfer` — header `Idempotency-Key` required (NFR-CONC-02).

```json
{ "sourceWarehouseId": "wh-main-uuid", "destinationWarehouseId": "wh-annex-uuid", "quantity": 50, "reason": "Event stock rebalance", "referenceNote": "Ahead of Fall Fundraiser" }
```

Response `200` (note: **no `version` field anywhere in this exchange** — see Section 5):

```json
{
  "transferId": "xfer-uuid",
  "inventoryItemId": "inv-item-uuid",
  "quantity": 50,
  "sourceTransaction": { "warehouseId": "wh-main-uuid", "type": "TRANSFER_OUT", "quantityAfter": 150 },
  "destinationTransaction": { "warehouseId": "wh-annex-uuid", "type": "TRANSFER_IN", "quantityAfter": 80 },
  "performedBy": "elena.inv-mgr",
  "performedAt": "2026-07-09T13:00:00Z"
}
```

Insufficient stock: `409 Conflict`, `errorCode: INSUFFICIENT_STOCK`, `detail` states requested vs. `availableQuantity` (UC-INV-01 alternate flow) — computed and rejected atomically within the same `UPDATE ... WHERE quantity >= :n` operation, never via a separate read-then-check.

## 4.8 Multi-Currency Asset Registration + Depreciation Report (FR-INV-10)

Multi-currency capture at registration is shown inline in Section 4.1's `purchase` object (`amount`, `currency`, `fxRateToReportingCurrency`, `fxRateAsOfDate`). Per FR-INV-10, the **stored** reporting-currency amount computed at entry time — not a rate looked up later — is what every aggregate uses.

`GET /api/v1/reports/depreciation?fiscalYear=2026&categoryId=...`

```json
{
  "data": [
    {
      "assetId": "c4d9e2f1-....",
      "assetNumber": "AST-2026-004821",
      "category": "IT Equipment",
      "originalCurrency": "EUR",
      "originalAmount": 1100.00,
      "fxRateToReportingCurrency": 1.08,
      "fxRateAsOfDate": "2026-01-05",
      "reportingCurrency": "USD",
      "reportingAmount": 1188.00,
      "depreciationMethod": "STRAIGHT_LINE",
      "usefulLifeMonths": 48,
      "salvageValue": 100.00,
      "accumulatedDepreciation": 297.00,
      "netBookValue": 891.00
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 640, "totalPages": 32 },
  "totals": {
    "reportingCurrency": "USD",
    "totalOriginalCostReportingCurrency": 188120.00,
    "totalAccumulatedDepreciation": 41500.00,
    "totalNetBookValue": 146620.00,
    "note": "Aggregate totals use each transaction's stored reporting-currency amount as of entry (FR-INV-10), not a rate recomputed at report time."
  }
}
```

# 5. Concurrency Contract

Two distinct mechanisms are used, deliberately not unified into one, per SRS NFR-CONC-01/02:

## 5.1 Optimistic Locking — Entity Updates (assets, audit findings, lifecycle events)

Every versioned resource returns a `version` integer. The client must echo the `version` it last read in any `PATCH`:

```json
PATCH /api/v1/assets/c4d9e2f1-....
{ "version": 4, "status": "UNDER_REPAIR" }
```

On a stale write, `409 Conflict`:

```json
{
  "type": "https://iams.internal/problems/optimistic-lock-conflict",
  "title": "The resource was modified by another user",
  "status": 409,
  "detail": "This asset was updated to version 6 by another user since you last loaded it.",
  "instance": "/api/v1/assets/c4d9e2f1-....",
  "errorCode": "OPTIMISTIC_LOCK_CONFLICT",
  "expectedVersion": 4,
  "currentVersion": 6,
  "currentResource": { "id": "c4d9e2f1-....", "status": "IN_USE", "version": 6, "...": "full current server-side state" }
}
```

The client is expected to show the conflict to the user (or auto-merge non-conflicting fields), then retry with `currentVersion`. This is applied uniformly to `PATCH /assets/{id}`, `PATCH /audits/{id}`, `PATCH /audits/{id}/findings/{findingId}` (pre-submission only — post-submission is immutable, Section 3.5), `PATCH /inventory-items/{id}` (definition fields only), and all other entity `PATCH` endpoints listed in Section 3.

## 5.2 Atomic Row-Level Operations — Inventory Quantity Mutations

`POST /inventory-items/{id}/stock-in`, `/stock-out`, `/adjust`, `/transfer` **never** accept or return a `version` field. There is nothing for the client to "read then write back" — the server computes the new quantity atomically (`UPDATE ... SET quantity = quantity - :n WHERE quantity >= :n`, or a short `SELECT ... FOR UPDATE` transaction) and either succeeds or returns `409 INSUFFICIENT_STOCK` in the same call. This is why these are modeled as **action endpoints with request-scoped deltas** (`{"quantity": 50}`), not generic `PATCH /inventory-items/{id} {"quantityOnHand": ...}` — a client sending an absolute quantity under contention would silently clobber another concurrent Stock Out, which is exactly the failure mode NFR-CONC-02 exists to prevent. Retry-safety for these endpoints comes from `Idempotency-Key` (Section 1.7), not from optimistic locking.

# 6. File Upload/Download Contract

**Decision: all attachment traffic is streamed through the backend; no client ever receives a direct MinIO URL.** SRS Section 4.2 and Section 6.7 are explicit that the object store "shall not be directly reachable by end-user clients" and that uploads must be validated server-side before being written to storage. A presigned-URL-to-client pattern (common in cloud deployments) is therefore ruled out here — it would require exposing MinIO's endpoint to the browser, contradicting the Docker network segmentation in SRS Section 6.8 ("the database and object-store containers are not directly reachable from outside the Compose network; only the backend and reverse proxy are exposed"). The backend-brokered pattern also lets a single place enforce per-file authorization (an attachment inherits its parent asset's org-scope check) and content-type/malware-style validation (SRS 6.7) uniformly, rather than trusting a time-boxed URL not to be shared onward.

**Upload:** `POST /api/v1/assets/{id}/attachments` — `multipart/form-data`, fields `file`, `attachmentType` (`IMAGE`|`INVOICE`|`MANUAL`|`WARRANTY_CARD`|`OTHER`). Server validates file type (allow-list, e.g. `image/jpeg`, `image/png`, `application/pdf`) and size (configurable max, default 25MB) **before** writing to MinIO; a rejected file never reaches object storage.

```json
{
  "attachmentId": "att-uuid",
  "attachmentType": "INVOICE",
  "fileName": "vendor-invoice-4821.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 184320,
  "checksumSha256": "9f86d0...",
  "downloadUrl": "/api/v1/attachments/att-uuid",
  "uploadedBy": "elena.inv-mgr",
  "uploadedAt": "2026-07-09T09:05:00Z"
}
```

Rejections: `415` `errorCode: FILE_TYPE_NOT_ALLOWED`, or `413` `errorCode: FILE_TOO_LARGE`.

**Download:** `GET /api/v1/attachments/{attachmentId}` — the backend authorizes the request against the parent asset's org-scope/role rules, streams bytes from MinIO through itself, and sets `Content-Type`/`Content-Disposition: attachment; filename="..."` on the response. The URL is a stable backend path (`downloadUrl` above), never a MinIO-signed URL, so it works identically whether or not the caller currently holds a fresh signed token — the backend's own auth (JWT bearer) governs access at request time, which also means access can be revoked instantly (e.g., after offboarding) rather than waiting for a presigned URL to expire.

# 7. Integration and Webhook Contract

## 7.1 Enable/Disable

`PUT /api/v1/integrations/{key}` — `key` ∈ `LDAP`, `SSO`, `ACCOUNTING_EXPORT`, `HR_SYNC`, `SMS_GATEWAY`, `WEBHOOKS`. Disabled by default (FR-INT-05).

```json
{ "enabled": true, "config": { "...": "integration-specific fields" }, "complianceReviewedBy": "reyes.compliance-officer" }
```

`complianceReviewedBy` is mandatory (`400 VALIDATION_FAILED` otherwise) when `enabled: true` for any integration that constitutes an outbound data flow (`ACCOUNTING_EXPORT`, `HR_SYNC`, `SMS_GATEWAY`, `WEBHOOKS`) — enforcing BRD Section 6.5 ("Compliance Officer sign-off before any new integration is enabled") in software, not just in process. `LDAP`/`SSO` are inbound-authentication integrations and don't require this field.

## 7.2 Outbound Webhook Payload and Signature

`POST` to the organization-configured, allow-listed receiving URL (FR-INT-04, SRS Section 6.6 SSRF prevention — the URL is set by an Administrator via `PUT`/`POST /integrations/webhooks`, never accepted ad hoc from a request body elsewhere in the API):

```
POST https://receiver.example.org/iams-hook
Content-Type: application/json
X-IAMS-Event: audit.completed
X-IAMS-Delivery-Id: dlv-8823....
X-IAMS-Timestamp: 2026-07-09T10:00:00Z
X-IAMS-Signature: sha256=6f9e2c...
```
```json
{
  "event": "audit.completed",
  "deliveryId": "dlv-8823....",
  "occurredAt": "2026-07-09T10:00:00Z",
  "data": { "auditId": "audit-uuid", "auditName": "Q3 2026 Science Department Audit", "exceptionCount": 5, "certificateUrl": "/api/v1/audits/audit-uuid/certificate" }
}
```

`X-IAMS-Signature` is HMAC-SHA256 over the **raw request body bytes**, keyed with the per-webhook signing secret (FR-INT-06); the receiving system recomputes and compares using a constant-time comparison, per standard webhook-security practice. Delivery uses exponential backoff on non-2xx responses, capped at a configurable retry count; every attempt (including failures) is recorded and visible at `GET /integrations/webhooks/{id}/deliveries`. Supported `event` values (initial set): `audit.completed`, `asset.status_changed`.

## 7.3 Read-Scoped Accounting/ERP Export (FR-INT-01)

`GET /api/v1/integrations/accounting-export/depreciation?fiscalYear=2026&format=csv`

Authenticated by either an interactive `VIEWER`/`SUPER_ADMIN` session, or an `INTEGRATION_SVC` service-account credential scoped to `INT_ACCOUNTING_READ` only (FR-SEC-14 — never a blanket administrative credential). Returns a file stream (`Content-Type: text/csv` or `application/json` per `format`) mapped to a documented, stable column schema: `assetNumber, category, orgNode, purchaseDate, originalCurrency, originalAmount, reportingCurrencyAmount, depreciationMethod, accumulatedDepreciation, netBookValue, fiscalYear`. This endpoint is **read-only by construction** — there is no corresponding write path from the accounting system back into IAMS, matching SRS Section 6.6's "scoped service-account role limited to read access on financial/valuation data only."

# 8. Async/Long-Running Operation Pattern

Applied uniformly to bulk import commit (Section 4.3), bulk export (FR-MIG-02), large report exports (Section 3.7), and batch label generation (FR-RPT-11) — anywhere NFR-PERF-03/04 requires background processing rather than a blocking request thread.

**Pattern**: the triggering `POST` returns `202 Accepted` immediately with a `Job` resource and a `Location` header; the client polls `GET /api/v1/jobs/{jobId}` until the job reaches a terminal state.

```json
{
  "jobId": "job-uuid",
  "type": "REPORT_EXPORT",
  "status": "RUNNING",
  "progressPercent": 42,
  "submittedBy": "finance.viewer",
  "submittedAt": "2026-07-09T09:00:00Z",
  "startedAt": "2026-07-09T09:00:02Z",
  "completedAt": null,
  "resultUrl": null,
  "error": null
}
```

`status` ∈ `PENDING` | `RUNNING` | `COMPLETED` | `FAILED` | `CANCELLED`. On `COMPLETED`, `resultUrl` points to a backend-brokered download (same pattern as Section 6, never a direct object-store link). On `FAILED`, `error` carries a problem-detail-shaped object. `GET /api/v1/jobs` lists jobs for the current user (or all, for `SUPER_ADMIN`/`ADMIN`); `DELETE /api/v1/jobs/{jobId}` cancels a `PENDING`/`RUNNING` job where cancellation is supported by that job type. This is exactly the mechanism that satisfies UC-MIG-01's "if Priya closes the browser mid-import, the job continues server-side and she can check its status when she returns" — the job survives independently of any client connection because it's a server-side row, not a held HTTP connection or WebSocket stream.

**Judgment Call**: polling (not WebSockets/SSE/push) is used throughout, per Judgment Call #5 — a second real-time transport adds reverse-proxy configuration and operational surface area that works against the "keep operational complexity low" constraint (BRD 11.2) for the target IT-generalist deployment persona, for a UX benefit (a few seconds of latency on a progress bar) that doesn't justify the cost here.

# 9. OpenAPI Generation and Governance

## 9.1 Source of Truth

**springdoc-openapi (springdoc-openapi-starter-webmvc-ui) auto-generates the OpenAPI 3.0 document from Spring annotations at runtime** (`@Tag`, `@Operation`, `@Schema`, Bean Validation constraints on request DTOs, etc.) — per SRS Section 2.2 ("documented via OpenAPI/Swagger"). There is no hand-maintained, separately-versioned YAML spec file to keep in sync; the running application *is* the source of truth for the exact wire contract. **This document (IAMS-API-1.0)** is the upstream design contract used to scaffold controllers and DTOs and to review request/response shapes before code is written — once implemented, any divergence between this document and the generated spec is a bug to reconcile back into this document, not a reason to trust this document over the code.

## 9.2 Publication

- Machine-readable spec: `GET /v3/api-docs` (JSON), `GET /v3/api-docs.yaml`.
- Human-readable UI: Swagger UI at `/swagger-ui/index.html`.
- Both are **on-premises-only by default** (SRS Section 7.3: "Exposed on-premises only; not internet-facing by default") — the reverse-proxy configuration (SRS Section 2.5) does not route these paths to any internet-facing listener, and in the `prod` Spring profile both paths additionally require an authenticated `SUPER_ADMIN` session (Spring Security rule on `/v3/api-docs/**` and `/swagger-ui/**`), so even an internal user who reaches the network segment can't browse the full API surface without elevated privilege. Both are open (unauthenticated, for developer convenience) in the `dev`/`staging` Spring profiles only.

## 9.3 Versioning and Deprecation Policy

- The URI path segment (`/api/v1`) is the version boundary (Section 1.1). Additive, backward-compatible changes — new optional request fields, new endpoints, new response fields a well-behaved client ignores — ship continuously within `/api/v1` with no version bump.
- A breaking change (removed/renamed field, changed semantics, removed endpoint) requires a new major version, `/api/v2`, running **side-by-side** with `/api/v1` for a minimum **6-month overlap window**, matching NFR-MAINT-05's backward-compatibility-during-migration requirement.
- An endpoint being retired ahead of its version's end-of-life is marked `@Deprecated` in code (which springdoc surfaces as `deprecated: true` in the generated spec and visibly flags in Swagger UI) and returns the standard deprecation headers on every response, per RFC 8594: `Deprecation: true`, `Sunset: <date>`, `Link: <replacement-endpoint-doc-url>; rel="successor-version"`.
- Schema/contract changes to the API layer are exercised the same way as database schema changes (SRS NFR-MAINT-02): validated in Staging against a production-equivalent snapshot before Production, per SRS Section 2.6.

# 10. Open Items Flagged for Later Resolution

This document made the following judgment calls to remain implementation-ready despite gaps left open by the BRD/FRS/SRS/PUC. Each is a reasonable default under standard REST/OWASP ASVS practice, but is flagged here for explicit sign-off during backlog creation rather than being silently treated as settled:

1. **`IT_SECURITY_OFFICER` and `COMPLIANCE_OFFICER` role modeling** (Section "Judgment Call — Global Design Decisions," item 2): treated as pre-defined custom roles layered on FR-USR-02's mechanism, since FR-USR-01's eight default roles don't literally include them despite both being load-bearing stakeholders elsewhere in the BRD/SRS/PUC. Confirm this is the intended modeling before FR-USR-01 is implemented as an enum vs. a data-driven role table.
2. **SSO/LDAP authentication availability in R1 vs. module INT's R3 scope**: this document ships identity-provider authentication in R1 (under module SEC) while the rest of module INT ships in R3, to resolve an apparent tension between BR-07 (R1) and BR-16 (R3). Confirm this split with Product before sprint planning.
3. **`return-to-inventory` pulled into R1** ahead of the rest of module LIF, to make FR-USR-08 offboarding actually functional in R1. Confirm the minimal R1 semantics (unassign-only, no approval workflow) are acceptable versus deferring offboarding's asset-recovery step entirely to R2.
4. **Bulk-import commit does not require zero validation errors** (Section 4.3) — confirm this matches the intended UX; an organization could instead be required to reach a clean dry run before any commit is permitted.
5. **Hard-delete policy** (Judgment Call #4): the specific list of "no audit-trail obligation" resources eligible for true `DELETE` (saved searches, webhooks, draft purchase requests, unused custom roles) should be reviewed against the final data model once the ER diagram (a BRD-planned downstream artifact) exists.
6. **Max page size (200) and Idempotency-Key retention window (48 hours)** are this document's defaults, not sourced from an NFR — revisit against real load-testing results at 100,000+ assets (NFR-SCALE-01).
7. **Polling-only async pattern** (Section 8): acceptable per the stated operational-simplicity rationale, but should be revisited if UAT feedback shows users find progress-bar latency unacceptable for very large exports.
