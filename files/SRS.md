**Software Requirements Specification**

**Inventory Audit Management System (IAMS)**

*Architecture, Non-Functional Requirements, Security & Compliance*

Document ID: IAMS-SRS-4.0 \| Version: 4.0 \| Status: Draft for Review (PM Adversarial Review Remediated)

# 1. Introduction

## 1.1 Purpose

This Software Requirements Specification (SRS) defines the technical architecture, non-functional requirements, security architecture, and interface requirements for the Inventory Audit Management System (IAMS). It complements the BRD (business intent, IAMS-BRD-4.0) and FRS (functional scope, IAMS-FRS-4.0) with the engineering detail needed to begin architecture and database design.

## 1.2 Scope

This document covers system architecture, technology stack, tenancy model, data architecture, non-functional requirements (performance, scalability, availability, maintainability, accessibility, internationalization), a dedicated Security Architecture section covering application, data, and integration layers, external interface requirements, and deployment requirements. Detailed API contracts and the full database schema are separate downstream artifacts.

## 1.3 Summary of Version 2.0 Changes

This revision closes gaps identified during adversarial review: explicit RTO/RPO targets, file/attachment storage architecture, WCAG 2.1 AA accessibility requirement, observability/monitoring, upgrade and schema-versioning strategy, concurrency control, browser support matrix, API rate limiting, timezone and localization architecture, a staging/test environment requirement, and a substantially expanded Security Architecture (Section 6) covering integration-layer controls end to end.

## 1.4 Summary of Version 4.0 Changes

This revision closes a third pass of adversarial-review findings: split concurrency control into optimistic locking (entity edits) vs. atomic row-level operations (high-contention inventory quantity mutations — NFR-CONC-02); required a scripted, single-command restore tool rather than a purely manual runbook, given the target persona's limited IT sophistication (NFR-AVAIL-03 amendment); clarified that the performance SLA explicitly covers append-only history/audit-trail queries, not just entity list/search (NFR-PERF-05); and added the dedicated-column-vs-JSONB data modeling rule referenced by FRS FR-AST-13/14 (Section 4.1).

# 2. System Architecture Overview

## 2.1 Architectural Style

IAMS shall follow a layered, modular monolith architecture at initial release, organized internally by Domain-Driven Design (DDD) principles — bounded contexts per functional module — rather than a distributed microservices architecture. This favors operational simplicity for on-premises deployment by small IT teams, while keeping module boundaries clean enough to extract services later if scale requires it.

## 2.2 Logical Layers

- Presentation Layer: React SPA (Material UI), consuming REST APIs, packaged as a Progressive Web App.

- API Layer: Spring Boot REST controllers, versioned (/api/v1/...), documented via OpenAPI/Swagger, fronted by rate limiting (Section 5.5).

- Application/Service Layer: use-case orchestration, transaction boundaries, authorization checks, integration orchestration.

- Domain Layer: entities, value objects, domain services, per DDD bounded context.

- Persistence Layer: Spring Data JPA / Hibernate repositories over PostgreSQL; object storage for file attachments (Section 4).

- Cross-Cutting: Spring Security (authN/authZ, JWT, LDAP/AD/SSO), centralized logging/exception handling, notification dispatch, audit-trail interceptors, secrets management, observability.

## 2.3 High-Level Component View

The backend is organized into modules corresponding to the FR module codes in the FRS: Asset (AST), Inventory (INV), Organization (ORG), Lifecycle (LIF), Audit (AUD), Scanning (SCN), Reporting (RPT), Dashboard (DSH), User/RBAC (USR), Notification (NTF), Search (SRC), Security (SEC), Data Migration (MIG), Integrations (INT), and Compliance (CMP). Modules communicate through well-defined service interfaces within the same deployable unit; no module directly accesses another module's database tables. The Integrations module (INT) is the single point through which any external system communicates with IAMS, so that all cross-boundary security controls (Section 6.6) are enforced in one place rather than scattered per module.

## 2.4 Tenancy Model

Per BRD Section 5, IAMS uses a single-tenant-per-instance model: one running instance serves one organization, which may itself contain multiple sites represented via the organizational hierarchy. The database schema therefore does not require a tenant-isolation column; hierarchy-based scoping (org_node_id on scoped entities) is sufficient and is enforced identically to permission scoping (FR-USR-04).

## 2.5 Deployment Architecture

The system shall be packaged as a set of Docker containers orchestrated via Docker Compose:

- iams-backend: Spring Boot application (Java 21), exposing REST APIs.

- iams-frontend: React application served via a lightweight web server (e.g., Nginx), or served statically by the backend for small deployments.

- iams-db: PostgreSQL database with a persistent volume for data.

- iams-object-store: on-premises S3-compatible object storage (e.g., MinIO) for asset images and file attachments, with its own persistent, encrypted volume (Section 4.2).

- iams-reverse-proxy: Nginx/Traefik for TLS termination, routing, and rate limiting; mandatory in production (Section 6.3).

The Compose stack shall run on both Linux and Windows Server hosts with Docker/Docker Desktop or a Windows-compatible container runtime. No component shall require outbound internet access for core functionality; the only acceptable outbound dependencies are optional, explicitly configured ones (Section 5.3) that degrade gracefully when disabled.

## 2.6 Environments

Each deployment shall provision, at minimum, a Staging environment in addition to Production. Staging shall be used to validate schema migrations, version upgrades, and security patches before they are applied to Production (Section 5.8). A Staging environment may be a smaller-capacity instance of the same Docker Compose stack; it is not required to match Production scale.

# 3. Technology Stack

| **Layer**                | **Technology**                                                                                                |
|--------------------------|---------------------------------------------------------------------------------------------------------------|
| Backend Language/Runtime | Java 21                                                                                                       |
| Backend Framework        | Spring Boot 3                                                                                                 |
| Security                 | Spring Security, JWT, optional LDAP/Active Directory, optional SAML2/OIDC SSO                                 |
| Persistence              | Spring Data JPA, Hibernate                                                                                    |
| Schema Migrations        | Flyway (version-controlled, automated)                                                                        |
| Build Tool               | Maven                                                                                                         |
| API Style / Docs         | REST, OpenAPI 3 / Swagger UI                                                                                  |
| Frontend Framework       | React                                                                                                         |
| UI Component Library     | Material UI                                                                                                   |
| Frontend Routing         | React Router                                                                                                  |
| HTTP Client              | Axios                                                                                                         |
| Frontend Delivery        | Responsive design, Progressive Web App (PWA) with offline-friendly audit scanning                             |
| Database                 | PostgreSQL                                                                                                    |
| Object Storage           | On-premises S3-compatible store (e.g., MinIO) for attachments/images                                          |
| Containerization         | Docker, Docker Compose                                                                                        |
| Reverse Proxy / TLS      | Nginx or Traefik                                                                                              |
| Target Hosts             | Linux servers, Windows Server                                                                                 |
| Observability            | Structured JSON logging with correlation IDs; Prometheus-compatible metrics endpoint; container health checks |

# 4. Data Architecture

## 4.1 Data Modeling Principles

- Every business entity (Asset, Audit, Transaction) carries immutable audit-relevant fields: created_by, created_at, updated_by, updated_at.

- History/trail data (asset history, audit findings, movement log) is append-only; corrections are new records referencing the original, never in-place edits (supports FR-AUD-18, FR-CMP-06 legal hold).

- Custom fields (FR-AST-06) are implemented via a flexible schema extension mechanism (e.g., a JSONB column per category) so new fields do not require migrations for every organization's configuration. **Dedicated-column-vs-JSONB rule (v4.0):** an attribute is modeled as a first-class, indexed column — never JSONB — when it must be efficiently filterable, sortable, or the subject of a date-driven notification trigger at asset-collection scale (e.g., FR-AST-13 insurance expiry, FR-AST-14 vehicle registration/insurance expiry). JSONB custom fields remain reserved for attributes that are organization-specific, not queried in bulk across the collection, and not the trigger condition for a notification or a standard report filter. Every FR that introduces a "specialized attribute" set shall state explicitly which category it falls into, per this rule.

- Organizational hierarchy (FR-ORG-01) is modeled as a generic, recursive tree structure rather than fixed columns, so it can represent Campus/Building/Room or Ministry/Department equally, and supports multi-site scoping (Section 2.4).

- Personal data fields (employee/volunteer/student-linked records) are tagged at the schema level as personal-data columns, so the anonymization workflow (FR-CMP-02) and retention policy engine (FR-CMP-01) can operate generically across tables without hardcoding table-specific logic.

- A full Database ER Diagram and Data Dictionary is produced as a separate downstream artifact building directly on these principles.

## 4.2 File and Attachment Storage

Asset images and file attachments (FR-AST-05) shall be stored in an on-premises, S3-compatible object store (e.g., MinIO), not as database BLOBs and not on an unmanaged local filesystem path. The database stores only a reference (object key, content hash, content type, size) to each file. This choice keeps the PostgreSQL database itself smaller and easier to back up/restore quickly, while the object store is backed up on its own schedule with the same RPO target as the database (see Section 5.3, Availability, Backup, and Disaster Recovery). All objects shall be encrypted at rest (see Section 6.4, Data Protection at Rest) and access-controlled through the backend — the object store shall not be directly reachable by end-user clients.

## 4.3 Data Retention and Purge

Retention periods are configurable per entity type per FR-CMP-01. The default posture, absent organization-specific configuration, is: security/login logs retained 1 year; audit records and their evidence retained indefinitely unless a legal-hold flag requires otherwise, since audit history has ongoing compliance value; former-employee/volunteer personal data anonymized (not hard-deleted) 2 years after departure, per FR-CMP-02, preserving the integrity of historical asset/audit totals that reference them.

# 5. Non-Functional Requirements

## 5.1 Performance

| **ID**      | **Requirement**                                                                                                                                                                  |
|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-PERF-01 | Standard list/search API responses shall return within 500ms (p95) for a database containing up to 100,000 assets, given appropriate indexing.                                   |
| NFR-PERF-02 | Barcode/QR scan-to-resolution during an audit shall complete within 1 second (p95) under normal on-premises network conditions.                                                  |
| NFR-PERF-03 | Bulk operations (label generation, bulk import/export, sample-based audit computation) shall run asynchronously with progress reporting rather than blocking the request thread. |
| NFR-PERF-04 | Report generation for large exports (e.g., full 100,000-asset register) shall be handled via background/streaming generation to avoid timeouts and excessive memory use.         |
| NFR-PERF-05 | The 500ms p95 target in NFR-PERF-01 applies to current-state entity list/search only. Append-only history and audit-trail queries (asset history, movement log, audit findings — the tables expected to reach 1,000,000+ rows per NFR-SCALE-01) are explicitly a separate, harder case and are governed instead by their own target: a bounded, filtered history query (e.g., one asset's full history, or one audit's findings) shall return within 1 second (p95) at 1,000,000+ rows given appropriate indexing (asset_id, audit_id, created_at); an unbounded/unfiltered history query across the entire collection is not a supported synchronous operation and shall instead follow the background/streaming pattern of NFR-PERF-04. |

## 5.2 Scalability

| **ID**       | **Requirement**                                                                                                                                                                                        |
|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-SCALE-01 | The data model and API layer shall support at least 100,000 asset records and 1,000,000+ historical transaction/audit-trail records without redesign.                                                  |
| NFR-SCALE-02 | The system shall support at least 100 concurrent active users per deployment at initial release, with a documented scaling path (vertical scaling, connection pool tuning, read replicas) beyond that. |
| NFR-SCALE-03 | Database schema shall use appropriate indexing, and pagination shall be mandatory (not optional) on all list endpoints returning asset-scale collections.                                              |

## 5.3 Availability, Backup, and Disaster Recovery

| **ID**       | **Requirement**                                                                                                                                                                                                                                                                      |
|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-AVAIL-01 | The system shall target 99.5% uptime during an organization's defined operating hours, excluding scheduled maintenance windows.                                                                                                                                                      |
| NFR-AVAIL-02 | Recovery Point Objective (RPO): no more than 15 minutes of data loss, achieved via continuous WAL archiving/log shipping in addition to full nightly backups of the database and object store.                                                                                       |
| NFR-AVAIL-03 | Recovery Time Objective (RTO): full service restoration within 4 hours of a declared incident. Given the target persona (often a part-time IT generalist, not a dedicated DBA — BRD Section 11.2's low-operational-complexity constraint), the 4-hour target shall be met via a single-command, scripted restore tool (e.g., a `restore.sh`/`restore.ps1` shipped with the Docker Compose stack that rehydrates the database and object store from the latest verified backup) rather than a purely manual, multi-step runbook; the manual runbook is retained as a documented fallback for the scripted tool's own failure modes, not as the primary path. |
| NFR-AVAIL-04 | Backup integrity shall be verified by an actual restore test at least quarterly (BRD Section 10); an unverified backup is not considered a satisfied requirement.                                                                                                                    |
| NFR-AVAIL-05 | The offline audit scan queue shall persist scans locally on the client device and guarantee eventual synchronization without data loss when connectivity to the on-premises server is restored; this covers short, localized connectivity gaps, not extended disconnected operation. |

## 5.4 Security (Summary — see Section 6 for full Security Architecture)

| **ID**     | **Requirement**                                                                                                                            |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-SEC-01 | All authentication uses JWT tokens with configurable expiry/refresh; passwords hashed using a strong, salted algorithm (bcrypt or Argon2). |
| NFR-SEC-02 | All authorization checks are enforced server-side; frontend role-based UI hiding is a UX convenience only.                                 |
| NFR-SEC-03 | Sensitive data is encrypted at rest (AES-256) and in transit (TLS 1.2+, TLS 1.3 preferred).                                                |
| NFR-SEC-04 | All security-relevant events are written to an immutable, centrally reviewable log.                                                        |

## 5.5 API Protection

| **ID**     | **Requirement**                                                                                                                                                                                            |
|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-API-01 | The reverse proxy / API gateway layer shall enforce rate limiting per authenticated user and per API key, with configurable thresholds, to prevent abuse and accidental overload from bulk client scripts. |
| NFR-API-02 | Rate-limit and throttling violations shall be logged and shall return a standard 429 response with a Retry-After header, not a silent failure.                                                             |

## 5.6 Concurrency

| **ID**      | **Requirement**                                                                                                                                                                                                                                                  |
|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-CONC-01 | Entity updates (asset records, audit findings, lifecycle events) shall use optimistic locking (a version column) so that two concurrent edits — e.g., two auditors updating the same asset — are detected and surfaced as a conflict rather than silently overwriting one another. This is appropriate here because conflicting edits to the same asset/audit record are rare and a user-facing "please retry" conflict is an acceptable outcome. |
| NFR-CONC-02 | Inventory quantity mutations (FR-INV-02 Stock In/Out, FR-INV-05 Adjustments, FR-INV-08 Transfer) are high-contention by nature (multiple staff adjusting the same warehouse item) and shall NOT rely on optimistic locking alone, which would produce an unacceptable retry rate under contention. Instead, quantity changes shall be applied as atomic, row-level database operations (e.g., a conditional `UPDATE ... SET quantity = quantity - :n WHERE quantity >= :n`, or `SELECT ... FOR UPDATE` within a short transaction) so that a Stock Transfer's decrement-source/increment-destination pair (FR-INV-08) is guaranteed atomic under concurrent load, per UC-INV-01. |

## 5.7 Accessibility

| **ID**     | **Requirement**                                                                                                                                                   |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-ACC-01 | The web application shall conform to WCAG 2.1 Level AA across all core workflows, verified by an accessibility audit before go-live (BRD Section 6.3, FR-CMP-04). |
| NFR-ACC-02 | The application shall be fully responsive across desktop, tablet, and mobile browser viewports.                                                                   |
| NFR-ACC-03 | Audit scanning screens shall be optimized for one-handed mobile use, given auditors frequently walk through facilities while scanning.                            |

## 5.8 Maintainability, Versioning, and Upgrades

| **ID**       | **Requirement**                                                                                                                                                                                                                                                                                 |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-MAINT-01 | Backend code shall follow SOLID principles and the module/bounded-context boundaries defined in Section 2.3.                                                                                                                                                                                    |
| NFR-MAINT-02 | Database schema changes shall be managed exclusively through automated, version-controlled Flyway migrations, applied first to Staging (Section 2.6) and verified before Production.                                                                                                            |
| NFR-MAINT-03 | The codebase shall maintain automated unit, integration, and end-to-end test coverage sufficient to safely refactor core modules, with coverage targets defined per module in the engineering backlog.                                                                                          |
| NFR-MAINT-04 | Centralized exception handling and structured logging (with correlation IDs) shall be implemented at the API gateway/controller layer.                                                                                                                                                          |
| NFR-MAINT-05 | Application version upgrades shall be backward-compatible with the prior schema version during a migration window, and shall be tested in Staging with a production-equivalent data snapshot before being applied to Production; a documented rollback procedure shall exist for every release. |

## 5.9 Observability

| **ID**     | **Requirement**                                                                                                                                                              |
|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-OBS-01 | The backend shall expose a health-check endpoint suitable for container orchestration liveness/readiness probes.                                                             |
| NFR-OBS-02 | The backend shall expose operational metrics (request rates, error rates, latency percentiles, queue depths for async jobs) in a Prometheus-compatible format.               |
| NFR-OBS-03 | Logs shall be structured (JSON) and include a correlation ID per request, enabling an administrator to trace a single user action end to end without direct database access. |

## 5.10 Internationalization and Time Zones

| **ID**      | **Requirement**                                                                                                                                                                                      |
|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-I18N-01 | The frontend shall externalize user-facing text into resource bundles to support translation into additional languages without code changes, even though only one language ships at initial release. |
| NFR-I18N-02 | All timestamps shall be stored in UTC and rendered in the viewing user's configured local time zone, which is required for organizations with sites in multiple time zones (BRD Section 5.2).        |
| NFR-I18N-03 | Number, date, and currency formatting shall respect a per-deployment locale setting.                                                                                                                 |

## 5.11 Portability / Deployment

| **ID**        | **Requirement**                                                                                                                                                                                                                                    |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-DEPLOY-01 | The full stack shall deploy via a single \`docker compose up\` on both Linux and Windows Server hosts.                                                                                                                                             |
| NFR-DEPLOY-02 | The system shall function with zero mandatory outbound internet connectivity; any optional integration requiring internet access shall degrade gracefully when unavailable.                                                                        |
| NFR-DEPLOY-03 | Configuration (DB credentials, LDAP/SSO settings, notification settings, integration credentials) shall be externalized via environment variables or an encrypted configuration store, never hardcoded (see also Section 6.5, Secrets Management). |

## 5.12 Browser Support

| **Browser**                                  | **Supported Versions**            |
|----------------------------------------------|-----------------------------------|
| Google Chrome / Chromium-based (Edge, Brave) | Latest and prior 2 major versions |
| Mozilla Firefox                              | Latest and prior 2 major versions |
| Safari (macOS and iOS)                       | Latest and prior 2 major versions |
| Internet Explorer                            | Not supported                     |

# 6. Security Architecture

This section is the authoritative security specification for IAMS, addressing the review finding that security was described only at a summary level and integration-layer security was absent entirely. It applies uniformly to the application, its data stores, and every external integration point.

## 6.1 Authentication

- Local authentication via username/password with JWT issuance, or delegated authentication via LDAP/Active Directory, or SSO (SAML 2.0 / OIDC) — configurable per deployment, with local authentication always available as a fallback for the Super Administrator account to prevent lockout if an external identity provider is unreachable.

- Passwords, where used, are hashed with bcrypt or Argon2 with a per-password salt; plaintext passwords are never logged or stored.

- JWT access tokens are short-lived; refresh tokens are longer-lived, revocable, and stored server-side in a manner that allows immediate revocation on logout or suspected compromise.

- Optional two-factor authentication (FR-SEC-03) uses a standard TOTP algorithm compatible with common authenticator apps.

## 6.2 Authorization

- Role-Based Access Control (RBAC) is enforced server-side on every API endpoint; role definitions and organizational-scope restrictions (FR-USR-04) are evaluated together, so a user's effective permission is the intersection of their role's permissions and their assigned hierarchy scope.

- Sensitive actions (permission changes, bulk export, retention-policy changes, legal-hold changes) require step-up re-authentication even within an active session (NFR-SEC session policy).

- Service accounts used by integrations (Section 6.6) are assigned their own scoped role — never an administrative credential — following the principle of least privilege.

## 6.3 Transport Security

- All traffic between clients and the reverse proxy, and between the reverse proxy and backend, uses TLS 1.2 or higher, with TLS 1.3 preferred where the organization's infrastructure supports it.

- The reverse proxy (Section 2.5) is the mandatory TLS termination point in production; direct unencrypted access to the backend container from outside the Docker network is not permitted.

- HTTP Strict Transport Security (HSTS) and secure/HttpOnly/SameSite cookie attributes are enforced for any session cookies.

## 6.4 Data Protection at Rest

- Database volumes and object-store volumes (Section 4.2) are encrypted at rest using AES-256, either via the underlying disk/volume encryption provided by the host OS or via database-native encryption, documented per deployment in the Installation Guide.

- Encryption keys are managed separately from the encrypted data — via a host-level key management facility or an external secrets manager — never stored alongside the database they protect.

- Backups (Section 5.3) inherit the same at-rest encryption as primary storage.

## 6.5 Secrets Management

- Database credentials, LDAP/SSO service-account credentials, integration API keys, and webhook signing keys are stored in a secrets manager or encrypted environment configuration, never committed to source control or written to plaintext log output.

- Secrets are rotatable without a full redeployment; the Administrator Guide documents the rotation procedure for each credential type.

## 6.6 Integration Security (End-to-End)

Every point where IAMS exchanges data with a system outside its own containers — LDAP/AD, SSO, accounting/ERP export, HR/SIS sync, SMS gateway, outbound webhooks — is treated as a distinct trust boundary and secured individually, addressing the review finding that integration security was previously unaddressed:

| **Integration Point**             | **Security Control**                                                                                                                                                 |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| LDAP / Active Directory           | Connection over LDAPS (TLS); service account is read-only and scoped to the minimum directory OU required                                                            |
| SSO (SAML/OIDC)                   | Signed and encrypted assertions; audience/issuer validation; no implicit trust of unsigned attributes                                                                |
| Accounting/ERP export (FR-INT-01) | Authenticated via signed API credentials or mutual TLS; scoped service-account role limited to read access on financial/valuation data only                          |
| HR/SIS sync (FR-INT-02)           | Authenticated pull or push with signed credentials; personal/student data mapped through the same anonymization-aware schema as native records (Section 4.1)         |
| Outbound webhooks (FR-INT-04)     | Payloads signed with HMAC (FR-SEC-10); receiving endpoint URL is allow-listed by an Administrator, not freely settable to prevent server-side request forgery        |
| SMS gateway (optional)            | Opt-in only (BRD Section 6.4); credentials in secrets manager; failure degrades gracefully with in-app/email fallback                                                |
| All integrations                  | Individually enable/disable-able (FR-INT-05), logged (FR-MIG-04 pattern extended to INT), and reviewed by the Compliance Officer before activation (BRD Section 6.5) |

## 6.7 Input Validation and Application Security

- All API inputs are validated server-side against defined schemas; validation is never enforced client-side only.

- The application is built and tested against the OWASP Top 10 and, where feasible, aligned to OWASP ASVS Level 2 controls (injection prevention via parameterized queries/JPA, output encoding to prevent XSS, CSRF protection on state-changing requests, secure file-upload handling with content-type and malware-scan validation for attachments).

- File uploads (FR-AST-05) are validated for file type and size before being written to object storage, and are served back to clients through the backend rather than directly, preventing stored-content from being executed in a browser context.

## 6.8 Dependency and Container Security

- Backend and frontend dependencies are scanned for known vulnerabilities as part of the CI/CD pipeline (FR-SEC-12); High/Critical findings block release until remediated or explicitly risk-accepted by the IT Security Officer.

- Container images are built from minimal, regularly-updated base images and are scanned for OS-level vulnerabilities before deployment.

- The Docker network is segmented so that the database and object-store containers are not directly reachable from outside the Compose network; only the backend and reverse proxy are exposed.

## 6.9 Security Logging, Monitoring, and Incident Response

- Authentication events (success/failure), authorization denials, administrative actions, data exports, and integration activity are logged immutably (FR-SEC-04) and are reviewable by the IT Security Officer.

- A documented incident response procedure (BRD Section 10) defines roles, containment steps, and notification timelines for a suspected data breach, consistent with applicable law.

- Periodic penetration testing or vulnerability assessment (FR-SEC-13) is recommended at least annually or after any major release, with findings tracked to closure.

## 6.10 Compliance Control Mapping

| **Compliance Area**                       | **Where Addressed**         |
|-------------------------------------------|-----------------------------|
| Data minimization / retention             | FR-CMP-01, Section 4.3      |
| Right to erasure / anonymization          | FR-CMP-02, Section 4.1      |
| Accessibility (WCAG 2.1 AA)               | FR-CMP-04, NFR-ACC-01       |
| Data residency (on-premises only)         | FR-CMP-05, Section 2.5      |
| Legal hold                                | FR-CMP-06, Section 4.1      |
| Financial reporting alignment (GAAP/IFRS) | FR-CMP-07, FR-AST-15        |
| Encryption at rest / in transit           | Sections 6.3, 6.4           |
| Least-privilege integration access        | Section 6.6                 |
| Incident response                         | Section 6.9, BRD Section 10 |

# 7. External Interface Requirements

## 7.1 User Interfaces

The primary interface is the React/Material UI single-page application described in Section 3. Two experience modes are required: a full administrative/desktop experience and a streamlined mobile/PWA audit-scanning experience, both conforming to WCAG 2.1 AA (Section 5.7).

## 7.2 Hardware Interfaces

- USB barcode scanners operating in keyboard-wedge (HID) mode.

- Bluetooth barcode scanners paired at the OS level, typically HID mode.

- Device cameras (Android, iOS, laptop webcam) accessed via the browser's media capture APIs for QR/barcode decoding client-side.

- Future: RFID readers, integrated via the scanning-abstraction interface (FR-SCN-06).

## 7.3 Software Interfaces

| **Interface**           | **Purpose**                    | **Security Control**                                                       |
|-------------------------|--------------------------------|----------------------------------------------------------------------------|
| LDAP / Active Directory | Optional authentication source | LDAPS, read-only scoped service account (Section 6.6)                      |
| SSO (SAML/OIDC)         | Optional authentication source | Signed/encrypted assertions (Section 6.6)                                  |
| SMTP Server             | Email notification delivery    | Authenticated relay, TLS where supported by the organization's mail server |
| SMS Gateway             | Optional SMS notifications     | Opt-in, secrets-managed credentials, graceful degradation                  |
| Accounting/ERP          | Depreciation/valuation export  | Signed API credentials or mutual TLS, read-scoped (Section 6.6)            |
| HR / SIS                | Roster synchronization         | Authenticated, anonymization-aware mapping (Section 6.6)                   |
| OpenAPI/Swagger UI      | API documentation              | Exposed on-premises only; not internet-facing by default                   |

## 7.4 Communication Interfaces

All communication between the frontend and backend occurs over HTTPS REST calls within the organization's internal network, through the reverse proxy (Section 2.5, 6.3). No component of the core system requires a connection outside that network.

# 8. Constraints

- Technology stack is fixed per Section 3 and may not be substituted without stakeholder re-approval.

- The system must not introduce a hard runtime dependency on any external SaaS or cloud service.

- The system must support restoring full functionality from an on-premises backup, within the RTO/RPO in Section 5.3, without vendor involvement.

- The system must conform to WCAG 2.1 AA and the compliance controls in Section 6.10 before go-live.

# 9. Assumptions

- The organization provides a host (physical or virtual machine) capable of running Docker with sufficient CPU/RAM/storage for its asset volume and attachment/object-storage growth; sizing guidance is provided in the Installation Guide.

- The organization's network provides reliable connectivity between client devices and the on-premises server within its own facilities; the offline scan queue (NFR-AVAIL-05) covers only short, localized connectivity gaps.

- Where an organization enables an integration (Section 6.6), it accepts responsibility for that third party's own security and data-handling practices, subject to the Compliance Officer review in BRD Section 6.5.

# 10. Appendix: Requirement Traceability Note

Non-functional and security requirements in this SRS apply globally across all functional requirements defined in the FRS. Where a specific FR has an unusually strict requirement beyond the global NFRs (for example, the sub-1-second scan resolution in FR-SCN-05, matched by NFR-PERF-02, or the compliance requirements in FR-CMP-01–07 matched by Section 6.10), that linkage is called out explicitly; all other FRs are governed by the general NFRs and Security Architecture in Sections 5 and 6 by default.
