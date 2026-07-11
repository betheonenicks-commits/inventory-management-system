# Software Requirements Specification (SRS)
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-SRS-2.0 | **Version:** 2.0 (Consolidated Development Baseline) | **Status:** For Ratification | **Date:** 2026-07-10

> **Supersedes** SRS v1.2 and all draft references to "IAMS-SRS-3.0/4.0". Changes vs v1.2: (a) the **MinIO object store** is added to the deployment architecture, network segmentation, backup scope, and RPO/RTO (it was mandated by DD 1.0/API 1.0 but absent here); (b) new NFR families **CONC** (concurrency), **OBS** (observability), **API** (rate limiting), and **NFR-MAINT-05** are formalized; (c) section cross-references cited by API 1.x are reconciled (Appendix A). All v1.2 requirements carry forward unchanged unless restated below.

## 1. Introduction
Purpose and scope as v1.2: this SRS defines architecture, technology stack, data-architecture principles, NFRs, external interfaces, and deployment. Detailed API contracts are in IAMS-API-1.1; the schema is in IAMS-DD-1.1.

## 2. System Architecture

**2.1 Style** — Layered **modular monolith**, DDD bounded contexts per module; services extractable later if scale demands. Favors operational simplicity per BRD §11.2.

**2.2 Logical layers** — React SPA (Material UI) as a PWA → versioned REST (`/api/v1`, OpenAPI/Swagger) → Spring Boot application/service layer (use-case orchestration, transaction boundaries, authorization) → domain layer per bounded context → Spring Data JPA/Hibernate over PostgreSQL. Cross-cutting: Spring Security (JWT, LDAP/AD, SSO), centralized exception handling, structured logging, notification dispatch, audit-trail interceptors.

**2.3 Components** — One deployable unit; modules AST, INV, ORG, LIF, AUD, SCN, RPT, DSH, USR, NTF, SRC, SEC, MIG, INT, CMP, ANL communicate through service interfaces; no module touches another module's tables.

**2.4 Deployment architecture (revised)** — Docker Compose stack:
- `iams-backend` — Spring Boot (Java 21) REST API
- `iams-frontend` — React app via Nginx (or served by backend for small deployments)
- `iams-db` — PostgreSQL with persistent volume
- **`iams-objectstore` — MinIO with persistent volume, storing all binary content (asset attachments, audit evidence photos, signature records, export files). NEW in 2.0.**
- `iams-reverse-proxy` (recommended for production) — Nginx/Traefik TLS termination and routing
Runs on Linux and Windows Server hosts. No component requires outbound internet; optional outbound dependencies (SMS gateway, NTP) only.

**2.5 Reverse proxy and network segmentation** — Only `iams-reverse-proxy` (or the backend, in proxy-less dev setups) is exposed outside the Compose network. **The database and object-store containers are not directly reachable by end-user clients**; all attachment traffic is brokered through the backend (API §6). Rate limiting (NFR-API-01) is enforced at this layer.

**2.6 Environments and change validation** — Three Spring profiles: `dev`, `staging`, `prod`. Database migrations and API contract changes are validated in `staging` against a production-equivalent snapshot before production rollout. Swagger UI and `/v3/api-docs` are open in dev/staging, and require an authenticated Super Administrator session in prod.

**2.7 Eventing & async** — In-process domain events backed by a **transactional outbox** table (at-least-once delivery) drive cross-module reactions (notifications, background report generation). Message broker (e.g., RabbitMQ) is a documented future option only.

## 3. Technology Stack
As v1.2, fixed: Java 21, Spring Boot 3, Spring Security (JWT, LDAP/AD, SAML2/OIDC), Spring Data JPA/Hibernate, Maven, REST + OpenAPI 3 (springdoc, generated from code), React + Material UI + React Router + Axios, PWA, PostgreSQL, **MinIO**, Docker/Docker Compose, Flyway (or Liquibase) migrations. Linux and Windows Server hosts. Stack substitutions require stakeholder re-approval (§7).

## 4. Non-Functional Requirements

**4.1 Performance** — NFR-PERF-01 list/search ≤500ms p95 at 100k assets; NFR-PERF-02 scan-to-resolution ≤1s p95; NFR-PERF-03 bulk ops async with progress; NFR-PERF-04 large report generation via background/streaming; **NFR-PERF-05 (new)** paginated history endpoints ≤500ms p95 at 1M history rows.

**4.2 Scalability** — NFR-SCALE-01 100,000 assets / 1,000,000+ history rows without redesign; NFR-SCALE-02 100 concurrent users with documented scaling path; NFR-SCALE-03 mandatory pagination on all list endpoints; **NFR-SCALE-04 (new)** audit-day burst: sustain 25 scan resolutions/second aggregate across concurrent audits while meeting NFR-PERF-02.

**4.3 Availability & reliability** — NFR-AVAIL-01 99.5% during defined operating hours excl. maintenance windows; **NFR-AVAIL-02 (revised)** automated backups on a configurable schedule covering **both PostgreSQL and the MinIO object store**, with a documented, tested restore procedure for the pair (evidence photos restore with their metadata); NFR-AVAIL-03 offline scan queue: durable client-side persistence and eventual synchronization without loss (see FR-AUD-19, now Must Have); **NFR-AVAIL-04 (revised)** RPO 24h / RTO 8h for a single-node deployment **including object-store content**; HA documented as future option; NFR-AVAIL-05 health endpoint + operational metrics; **NFR-AVAIL-06 (new)** crash consistency: an attachment/evidence write is acknowledged only after both the object-store write and its DB metadata row commit (write object first, commit metadata second; orphaned objects reaped by a scheduled janitor job).

**4.4 Security** — NFR-SEC-01 JWT with configurable expiry/refresh; bcrypt/Argon2 password hashing; NFR-SEC-02 all authorization server-side (UI hiding is UX only); NFR-SEC-03 sensitive data encrypted at rest, all traffic servable over TLS 1.2+; NFR-SEC-04 immutable security-event log; NFR-SEC-05 LDAP/AD optional, never a hard dependency; NFR-SEC-06 mandatory MFA for Super Admin/Admin (FR-SEC-03a); NFR-SEC-07 lockout (FR-SEC-09); NFR-SEC-08 configurable compliance postures per BRD §9; NFR-SEC-09 secrets via Docker secrets/restricted files/secrets manager, never in Compose files or VCS; JWT signing-key rotation without full redeployment; **NFR-SEC-10 (new)** upload validation: content-type allow-list and size limits enforced server-side before any object-store write; rejected files never reach storage; **NFR-SEC-11 (new)** SSRF prevention: outbound webhook URLs are Administrator-registered and allow-listed; the API never accepts ad hoc callback URLs in request bodies; **NFR-SEC-12 (new)** offline scan queue on devices stores no credentials and no personal data beyond asset identifiers and evidence captured by the auditor; queued data is protected by the device/browser storage sandbox and cleared on logout.

**4.5 Concurrency (new family)** — **NFR-CONC-01** optimistic locking (integer `version`) on mutable business entities (asset, audit, pre-submission findings, inventory item definitions); stale writes rejected with a conflict response carrying current state. **NFR-CONC-02** inventory quantity mutations are atomic row-level operations (`UPDATE … WHERE quantity >= :n` or short `SELECT … FOR UPDATE`), never read-then-write from the client; retry safety via idempotency keys.

**4.6 Observability (new family)** — **NFR-OBS-01** structured JSON logs with level, module, user (where lawful), and outcome; PII redacted per CMP tagging. **NFR-OBS-02** metrics: request rate, error rate, p95 latency, outbox/job queue depth, object-store and DB disk headroom, exposed for on-prem monitoring; built-in admin status page with threshold warnings (disk, failed jobs, failed notification deliveries). **NFR-OBS-03** every request carries a correlation ID (`traceId`) surfaced in error responses and logs end to end.

**4.7 API management (new family)** — **NFR-API-01** rate limiting per authenticated user and per integration key at the reverse proxy/gateway (default 120 req/min interactive, configurable), with standard rate-limit headers. **NFR-API-02** violations return 429 + `Retry-After` + problem-details body.

**4.8 Maintainability** — NFR-MAINT-01 SOLID + module boundaries; NFR-MAINT-02 versioned automated migrations only; NFR-MAINT-03 unit/integration/E2E coverage sufficient for safe refactoring (targets per module in engineering backlog); NFR-MAINT-04 centralized exception handling + structured logging with correlation IDs; **NFR-MAINT-05 (new)** API backward compatibility: breaking changes ship as a new major version running side-by-side for ≥6 months, with RFC 8594 deprecation headers on retiring endpoints.

**4.9 Usability & accessibility** — NFR-UX-01 fully responsive; NFR-UX-02 audit screens optimized for one-handed mobile use; NFR-UX-03 installable PWA; NFR-UX-04 WCAG 2.1 AA; NFR-UX-05 externalized strings (English-only Phase 1); NFR-UX-06 latest two stable Chrome/Edge/Firefox/Safari desktop + current Android/iOS default browsers; **NFR-UX-07 (new)** standard UI states specified for every listing/detail screen: loading, empty, error (with traceId), permission-denied, and background-job progress; session expiry mid-audit preserves the offline queue and returns to the audit after re-authentication.

**4.10 Portability/deployment** — NFR-DEPLOY-01 full stack via single `docker compose up` on Linux/Windows Server; NFR-DEPLOY-02 zero mandatory outbound internet; optional integrations degrade gracefully; NFR-DEPLOY-03 externalized configuration; secrets per NFR-SEC-09; **NFR-DEPLOY-04 (new)** upgrade procedure: versioned releases upgrade via image tag change + automatic migrations, with a documented pre-upgrade backup step and rollback-by-restore; an Installation & Operations Guide with host sizing (by asset volume), backup verification, and restore drill instructions is a release deliverable.

## 5. External Interface Requirements
**5.1 User interfaces** — desktop administrative experience + streamlined mobile/PWA audit-scanning experience (as v1.2). **5.2 Hardware** — USB/Bluetooth scanners in HID keyboard-wedge mode; device cameras via browser media-capture with client-side decode; label output per FR-SCN-07 via standard OS printing; future RFID via the scan abstraction. **5.3 Software interfaces** — LDAP/AD (optional), SMTP (org relay), SMS gateway (optional, degrade gracefully), OpenAPI/Swagger (on-prem only; prod requires Super Admin auth). **5.4 Communication** — HTTPS REST within the organization's network only.

## 6. Data Architecture Principles
As v1.2, with object-store additions: audit-relevant fields (`created_by/at`, `updated_by/at`) on every entity; history append-only, corrections as linked records; custom fields via JSONB validated by per-category JSON Schema; org hierarchy as recursive tree persisted with closure-table/materialized-path for descendant queries at scale; retention tiers per BRD §5.4; PII fields schema-tagged for CMP export/erasure. **New:** binary content lives only in MinIO — the database stores object keys, content types, sizes, and SHA-256 checksums; attachment access is always backend-brokered with the parent entity's authorization; checksums are verified on download to detect tampering/corruption.

## 7. Constraints
Fixed stack (§3); no hard runtime dependency on external SaaS/cloud; full functionality restorable from an on-premises backup (database + object store) without vendor involvement.

## 8. Assumptions
1. The organization provides a Docker-capable host sized per the Installation Guide. 2. Reliable in-facility connectivity between clients and the server; the offline queue covers **short, localized gaps** (target: up to one working day of queued scanning), not extended fully-disconnected operation.

## Appendix A — Cross-Reference Reconciliation for API 1.x Readers
API 1.0 cited draft-SRS section numbers that map to this document as follows: "SRS 4.1/4.2 (data architecture / object storage)" → **§6**; "SRS 6.1 (auth/JWT)" → **§4.4 NFR-SEC-01**; "SRS 6.2 (step-up)" → **§4.4**; "SRS 6.6 (SSRF / service accounts)" → **NFR-SEC-11 / FR-SEC-14**; "SRS 6.7 (upload validation)" → **NFR-SEC-10**; "SRS 6.8 (network segmentation)" → **§2.5**; "SRS 2.2/2.5/2.6" → **§2.2/§2.5/§2.6** (unchanged); "SRS 7.3 (Swagger exposure)" → **§5.3 and §2.6**; "NFR-CONC/OBS/API/MAINT-05" → **§4.5/§4.6/§4.7/§4.8**. API 1.1 retains its citations under this mapping.
