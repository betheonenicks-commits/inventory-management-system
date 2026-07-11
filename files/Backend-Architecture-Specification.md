**Backend Architecture & Data Model Specification**

**Inventory Audit Management System (IAMS)**

*Implementation-Ready Package Structure, Entity-Relationship Data Dictionary, and Cross-Cutting Mechanism Design*

Document ID: IAMS-BAS-1.0 | Version: 1.0 | Status: Draft for Engineering Implementation

Related Documents: IAMS-BRD-4.0 (Business Requirements), IAMS-FRS-4.0 (Functional Requirements), IAMS-SRS-4.0 (Architecture / NFR / Security), IAMS-PUC-1.0 (Personas & Use Cases)

# 0. How to Read This Document

This document is what backend engineers build from. It resolves every open modeling question left by the BRD/FRS/SRS with a concrete decision, stated explicitly with rationale where the source documents left it open. It does not restate business intent — read the BRD/FRS/SRS for *why* a capability exists; read this document for *how it is built*.

Every entity, mechanism, and package below is traceable to one or more FR-IDs. Where this document makes a judgment call the requirements docs leave open, it is called out in a **Decision** callout, not left implicit.

**Conventions used throughout this document:**

- `ENUM(...)` in a field type column denotes a small, closed set of values enforced by a `VARCHAR` column with a `CHECK` constraint and mirrored by a Java `enum`, **not** a native PostgreSQL `ENUM` type — see Section 12.4 for why.
- Every table listed carries `id UUID PRIMARY KEY` unless stated otherwise — see Section 2.0 for the PK strategy decision.
- "Audit columns" = `created_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_by UUID, updated_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0` unless a table is append-only (in which case only `created_by`/`created_at` apply — see Section 8).
- Target database: PostgreSQL 16. Extensions used: `pgcrypto` (or core `gen_random_uuid()` on PG16+), `ltree`, `pg_trgm`.
- Release tags (R1/R2/R3) follow BRD Section 8.2 and FRS Section 5 (FR-level Release Mapping appendix); where those documents are silent at the entity level, this document states the sequencing decision explicitly with rationale (Section 1.4).

# 1. Package / Module Structure

## 1.1 Top-Level Layout

Per SRS Section 2.1/2.3 (layered modular monolith, DDD bounded contexts, one per FRS module, no module reaches into another module's tables), the codebase is a single Maven multi-module-free project (one deployable JAR) organized as Java packages, not separate Maven modules — module boundaries are enforced by convention plus an ArchUnit test (Section 11.4), not by Maven's dependency graph, since a full Maven multi-module split adds build complexity disproportionate to a modular-monolith target of this scale.

```
com.iams
├── common/                      # Shared kernel — see Section 1.3
├── asset/                       # AST
├── inventory/                   # INV
├── org/                         # ORG
├── lifecycle/                   # LIF
├── audit/                       # AUD
├── scan/                        # SCN
├── reporting/                   # RPT
├── dashboard/                   # DSH
├── user/                        # USR
├── notification/                # NTF
├── search/                      # SRC
├── security/                    # SEC
├── migration/                   # MIG
├── integration/                 # INT
├── compliance/                  # CMP
├── analytics/                   # ANL
└── IamsApplication.java         # Spring Boot entry point
```

Each package above corresponds 1:1 to one FRS module code. This is a deliberate, literal mapping so that "which package owns this requirement" is never ambiguous during backlog grooming.

## 1.2 Internal Layering Per Module

Every module package follows the same four-layer internal structure, consistent with SRS Section 2.2 (Presentation / API / Application / Domain / Persistence):

```
com.iams.asset
├── api/
│   ├── AssetController.java              # @RestController, /api/v1/assets/**
│   ├── dto/
│   │   ├── AssetCreateRequest.java       # inbound DTO, JSR-380 annotated
│   │   ├── AssetResponse.java            # outbound DTO
│   │   └── AssetSummaryResponse.java
│   └── mapper/
│       └── AssetMapper.java              # MapStruct interface: entity <-> DTO
├── application/
│   ├── AssetRegistrationService.java     # interface (port)
│   ├── AssetRegistrationServiceImpl.java # use-case orchestration, @Transactional boundary
│   ├── AssetQueryService.java            # read-side, Pageable-based queries
│   └── event/
│       ├── AssetRegisteredEvent.java     # domain event, published via ApplicationEventPublisher
│       └── AssetStatusChangedEvent.java
├── domain/
│   ├── Asset.java                        # JPA entity / aggregate root
│   ├── AssetCategory.java
│   ├── AssetStatusDef.java
│   ├── AssetHistoryEvent.java
│   ├── ...                               # other entities/value objects for this module
│   ├── AssetRepository.java              # Spring Data JPA repository interface (port)
│   └── service/
│       └── AssetNumberGenerator.java     # domain service, no Spring dependency where feasible
└── infrastructure/
    └── persistence/
        └── (Spring Data JPA repository impls are auto-generated; custom native/JPQL
            queries live in *RepositoryCustom + *RepositoryCustomImpl here)
```

- **api**: controllers, request/response DTOs, MapStruct mappers. Never exposes JPA entities directly (Section 12.3).
- **application**: use-case services — the only layer allowed to open/own a `@Transactional` boundary; orchestrates domain objects and repositories, publishes domain events, performs SoD/authorization checks that need cross-entity context.
- **domain**: JPA entities, value objects, enums, repository *interfaces*, and pure domain services (business rules that don't need Spring).
- **infrastructure/persistence**: anything JPA/Hibernate-specific that isn't a plain Spring Data derived-query repository (native SQL for the atomic inventory operations in Section 8, `@Query` JPQL for recursive/ltree queries, Testcontainers-backed repository tests).

Other modules (`inventory`, `org`, `lifecycle`, `audit`, `scan`, `reporting`, `dashboard`, `user`, `notification`, `search`, `security`, `migration`, `integration`, `compliance`, `analytics`) each follow this identical `api / application / domain / infrastructure.persistence` structure. This document does not repeat the tree for every module — Section 2 lists each module's entities (which live under that module's `domain/` package).

## 1.3 Shared Kernel — `com.iams.common`

A small shared kernel is unavoidable in a DDD modular monolith (base entity, org-scope resolution, the file-storage port, personal-data tagging). It is kept intentionally thin — anything that could reasonably live inside one module's bounded context stays there instead.

```
com.iams.common
├── domain/
│   ├── BaseEntity.java             # id, version, created_by/at, updated_by/at (Section 2.0)
│   ├── OrgScopedEntity.java        # extends BaseEntity, adds org_node_id
│   ├── PersonalDataField.java      # @interface — marks a column as personal data (Section 11)
│   └── DomainEvent.java            # marker interface for all module domain events
├── event/
│   ├── DomainEventPublisher.java   # thin wrapper over ApplicationEventPublisher (Section 9)
│   └── OutboxEvent.java            # entity backing the webhook/at-least-once delivery outbox
├── security/
│   ├── CurrentUserProvider.java    # resolves the authenticated principal + org scope
│   └── OrgScopeEvaluator.java      # intersects role permission set with hierarchy scope (Section 6)
├── storage/
│   └── ObjectStoragePort.java      # interface over MinIO; impl lives in infrastructure
├── web/
│   ├── ApiExceptionHandler.java    # @ControllerAdvice (Section 12.5)
│   ├── ApiError.java
│   └── PageResponse.java           # standard envelope around Spring Data Page<T>
└── money/
    └── MonetaryAmount.java         # embeddable value object (Section 7)
```

`common` may be depended on by every module. No module depends on another module's `domain` or `infrastructure` packages directly — cross-module interaction is either (a) a public `application`-layer service interface injected via Spring, or (b) a domain event (Section 9). This is enforced by an ArchUnit rule in the test suite (`ModuleBoundaryTest`, Section 11.4).

## 1.4 Release Sequencing at the Entity Level

BRD Section 8.2 and FRS Section 5 sequence releases at the BR/FR level. Section 2 below tags every entity R1/R2/R3 so engineering can sequence Flyway migrations and sprints. Three sequencing calls are not fully determined by BRD/FRS and are resolved here explicitly:

**Decision D-1 — Asset "current assignment" ships thin in R1, full workflow in R2.** FR-RPT-03 (Employee Asset List) is R1 (basic reporting), which requires *some* notion of "what's assigned to whom" to exist in R1 — but FR-LIF-04/05 (the full Assignment/Transfer workflow with approval routing) is R2 under BR-04. Resolution: `Asset.assigned_to_employee_id` is a plain denormalized FK settable directly by an Administrator/Inventory Manager in R1 (no workflow, no approval, just a field edit recorded in `AssetHistoryEvent`). The full `AssetAssignment` history entity and `AssetTransferRequest` approval workflow (Section 2.4) ship in R2 and become the system of record for that same field going forward.

**Decision D-2 — Search: direct lookup is R1, global/advanced/saved search is R2.** No BR in BRD Section 4 explicitly owns the Search module. Since FR-SRC-02 (barcode/serial/asset-number direct lookup) is load-bearing for using the R1 asset register at all, it ships in R1 as a thin query against the `asset` table's indexed `asset_number`/`serial_number`/`barcode_value` columns. FR-SRC-01 (global cross-entity search), FR-SRC-03 (advanced combinable filters), and FR-SRC-04 (`SavedSearch`) ship in R2 alongside the full Reporting/Dashboard suite, once Employee, Vendor, and Purchase Order records they search across exist in volume.

**Decision D-3 — Product feedback (FR-ANL-04) ships in R1; usage-metrics collection (FR-ANL-01–03) ships in R2.** BRD Section 8.2 places all of BR-20 (usage analytics) in R2, but BRD Section 15 explicitly states the in-app feedback mechanism "shall be available to all roles from R1 onward." These are two different FRs inside the same FRS module with different release dates: `FeedbackSubmission` (FR-ANL-04) ships R1; `UsageMetricDaily`/`FeatureUsageEvent` (FR-ANL-01–03) ship R2.

**Decision D-4 — `IntegrationConfig` ships in R1 (to govern LDAP/SSO), the rest of module INT ships in R3.** BR-07 (RBAC with LDAP/AD and optional SSO) is R1, but BR-16 (accounting/ERP and HR/SIS integration) is R3, and both are functionally part of FRS module INT. The `IntegrationConfig` entity (Section 2.14) is the single governance record for "is this integration point enabled, by whom, with what sign-off" (FR-INT-05) and must exist in R1 to gate LDAP/SSO the same way it later gates ERP/HR/webhook integrations in R3 — it is not re-modeled twice.

# 2. Entity-Relationship Data Dictionary

## 2.0 Primary Key Strategy

**Decision: every table uses `UUID` primary keys, not `BIGSERIAL`.** Concretely: `id UUID PRIMARY KEY DEFAULT gen_random_uuid()` as the baseline, with **UUIDv7 generated application-side** (via a small library, e.g. `com.github.f4b6a3:uuid-creator`) preferred over database-default UUIDv4 wherever the application layer creates the row before persisting it.

Rationale:
- **Offline generation (FR-AUD-19, NFR-AVAIL-05).** The offline audit scan queue creates `ScanEvent` and `AuditFinding` rows client-side before the device ever reaches the server. A `BIGSERIAL` PK cannot be safely allocated offline (collision on sync); a UUID can, and a `client_generated_id UNIQUE` column absorbs de-duplication on reconnect (Section 2.6).
- **Bulk import (FR-MIG-01).** Import jobs stage thousands of rows that must get durable, referenceable IDs before commit (dry-run mode, FR-MIG-03) without contending on a shared sequence.
- **No information leakage.** A sequential integer asset ID printed on a QR code label (FR-AST-02) discloses the organization's total registered-asset count to anyone who scans a label; a UUID does not.
- **UUIDv7 over UUIDv4** is used specifically to avoid the B-tree index-fragmentation cost random UUIDs are known for at 100,000+ row / 1,000,000+ history-row scale (NFR-SCALE-01) — UUIDv7 is time-ordered in its high bits, giving it the sequential-insert locality of `BIGSERIAL` while keeping every other advantage above. Where UUIDv7 generation is impractical (e.g., a raw SQL `DEFAULT`), fall back to `gen_random_uuid()` — correctness is unaffected, only insert locality.

All foreign keys are therefore `UUID`. Business-facing identifiers that users actually see and search by (`asset_number`, PO number, audit certificate number) are separate, human-readable `VARCHAR` columns with their own unique index — never the PK.

## 2.1 Common / Shared Entities

### `outbox_event` (R1)
Purpose: at-least-once delivery backbone for domain-event side effects that must survive a process restart (notification dispatch, webhook delivery, dashboard cache invalidation) — see Section 9.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| aggregate_type | VARCHAR(100) | e.g. `Asset`, `AuditDefinition` |
| aggregate_id | UUID | id of the entity that raised the event |
| event_type | VARCHAR(150) | e.g. `AssetRegisteredEvent` |
| payload | JSONB | serialized event body |
| status | VARCHAR(20) | `PENDING`, `DISPATCHED`, `FAILED` |
| attempt_count | INTEGER | default 0 |
| next_attempt_at | TIMESTAMPTZ | for backoff scheduling |
| created_at | TIMESTAMPTZ | |
| dispatched_at | TIMESTAMPTZ | nullable |

Relationships: none (polymorphic reference via `aggregate_type`/`aggregate_id`, intentionally not an FK — the whole point of the outbox is to decouple from the source aggregate's transaction). Audit columns: `created_at` only (append-only).

### `personal_data_field_registry` (R1)
Purpose: schema-level catalog of which columns hold personal data, populated at startup by scanning `@PersonalDataField`-annotated JPA attributes (Section 11), so the retention/anonymization engine (FR-CMP-01/02) operates generically instead of hardcoding table names.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| table_name | VARCHAR(100) | |
| column_name | VARCHAR(100) | |
| data_category | VARCHAR(50) | e.g. `NAME`, `CONTACT`, `GOVERNMENT_ID` |
| entity_type | VARCHAR(100) | owning JPA entity class simple name |
| purpose_basis | TEXT | why this field is collected (FR-CMP-03) |

Relationships: none (metadata table). Audit columns: `created_at`, `updated_at` only.

## 2.2 Organization Management (ORG)

### `org_node_type` (R1)
Purpose: configurable catalog of hierarchy-level "kinds" (Campus, Building, Room, Department, Ministry, Cost Center...) — supports FR-ORG-02 relabeling without a schema change.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| code | VARCHAR(50) | stable machine code, e.g. `BUILDING` |
| display_label | VARCHAR(100) | organization-configurable label shown in UI |
| is_physical_location | BOOLEAN | true for Campus/Building/Room; false for Department/Cost Center |
| sort_order | INTEGER | |
| is_active | BOOLEAN | |

Relationships: referenced by `org_node.node_type_id`. Audit columns: full.

### `org_node` (R1)
Purpose: the recursive organizational hierarchy tree (FR-ORG-01/02/05) — see Section 3 for the chosen representation (adjacency list + `ltree`). A single tree carries both physical-location branches (Campus > Building > Room) and non-physical branches (Department/Cost Center, FR-ORG-03), distinguished by `node_type.is_physical_location`.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| parent_id | UUID | FK `org_node.id`, nullable (null = root) |
| node_type_id | UUID | FK `org_node_type.id` |
| name | VARCHAR(200) | |
| code | VARCHAR(50) | unique within siblings |
| path | LTREE | materialized path, maintained by trigger (Section 3) |
| depth | INTEGER | denormalized from `path`, indexed |
| location_attributes | JSONB | small ad hoc attributes for specialized location types (FR-ORG-06), e.g. classroom capacity |
| display_order | INTEGER | |
| is_active | BOOLEAN | |

Relationships: self-referencing FK (`parent_id`); FK to `org_node_type`; every `*_org_node_id` FK across the entire schema points here. Audit columns: full (`@Version` — structural moves use optimistic locking per Section 8).

Indexes: GiST on `path` (subtree/ancestor queries), B-tree on `(parent_id)`, unique `(parent_id, code)`.

### `employee` (R1)
Purpose: Employee/Volunteer (and, in education deployments, student-linked) records (FR-ORG-04). Personal-data fields are explicitly tagged (Section 11).

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| org_node_id | UUID | FK `org_node.id` (department/cost-center node) |
| person_type | VARCHAR(20) | `EMPLOYEE`, `VOLUNTEER`, `STUDENT` |
| employee_number | VARCHAR(50) | unique, nullable |
| first_name | VARCHAR(150) | `@PersonalDataField(category=NAME)` |
| last_name | VARCHAR(150) | `@PersonalDataField(category=NAME)` |
| email | VARCHAR(255) | `@PersonalDataField(category=CONTACT)`, unique nullable |
| phone | VARCHAR(30) | `@PersonalDataField(category=CONTACT)`, nullable |
| hire_date | DATE | nullable |
| termination_date | DATE | nullable |
| status | VARCHAR(20) | `ACTIVE`, `OFFBOARDING`, `OFFBOARDED`, `ANONYMIZED` |
| user_account_id | UUID | FK `app_user.id`, nullable (not every employee has login access) |
| legal_hold | BOOLEAN | default false (FR-CMP-06) |
| anonymized_at | TIMESTAMPTZ | nullable |

Relationships: FK to `org_node`; optional 1:1 to `app_user`; referenced by `asset.assigned_to_employee_id`, `asset_assignment`, `offboarding_case`. Audit columns: full.

## 2.3 Asset Management (AST)

**Decision D-5 — one append-only history table, not two.** FR-AST-10 (general change history) and FR-AST-11 (movement/location log) are modeled as a **single** table, `asset_history_event`, with an `event_type` discriminator that includes `LOCATION_CHANGE`. FR-RPT-07 (Asset Movement report) is simply a query filtered to `event_type = 'LOCATION_CHANGE'`. Modeling these as two parallel tables would require every location-changing code path to dual-write and risks the two logs drifting — a direct violation of "complete, immutable history" being trustworthy. FR-LIF-10 (lifecycle transaction history) is satisfied the same way: the Lifecycle module never writes to `asset_history_event` directly (that would violate the no-cross-module-table-access rule, Section 2.3 SRS) — it publishes a domain event that Asset module's own listener persists as a new `asset_history_event` row (Section 9).

### `asset_category` (R1)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| parent_category_id | UUID | FK self, nullable |
| name | VARCHAR(150) | |
| code | VARCHAR(50) | unique |
| requires_insurance_fields | BOOLEAN | gates UI display of `asset_insurance_detail` |
| requires_vehicle_fields | BOOLEAN | gates UI display of `vehicle_detail` |
| default_depreciation_method | VARCHAR(30) | `STRAIGHT_LINE`, `DECLINING_BALANCE`, nullable — R2 |
| default_useful_life_months | INTEGER | nullable — R2 |
| default_salvage_value_pct | NUMERIC(5,2) | nullable — R2 |
| is_active | BOOLEAN | |

Audit columns: full.

### `asset_status_def` (R1)
Purpose: configurable status list (FR-AST-07) — a reference table, not a DB enum, so an Administrator can add a status without a deployment.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| code | VARCHAR(30) | unique, e.g. `IN_USE`, `IN_STORAGE`, `UNDER_REPAIR`, `MISSING`, `RETIRED`, `DISPOSED` |
| label | VARCHAR(100) | |
| is_terminal | BOOLEAN | true for `RETIRED`/`DISPOSED` — excludes asset from future audit scopes |
| sort_order | INTEGER | |

Audit columns: full.

### `asset_custom_field_definition` (R1)
Purpose: organization-defined custom fields per category (FR-AST-06) — the schema *description*; actual values live in `asset.custom_attributes` JSONB (Section 4).

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| category_id | UUID | FK `asset_category.id` |
| field_key | VARCHAR(100) | JSON key used in `asset.custom_attributes`, unique per category |
| label | VARCHAR(150) | |
| data_type | VARCHAR(20) | `TEXT`, `NUMBER`, `DATE`, `BOOLEAN`, `ENUM` |
| is_required | BOOLEAN | |
| enum_options | JSONB | array of allowed values, only for `data_type = 'ENUM'` |
| display_order | INTEGER | |

Audit columns: full.

### `asset` (R1)
Purpose: the core asset record (FR-AST-01/03/04/06/07/09).

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_number | VARCHAR(50) | system-generated, unique, human-facing (FR-AST-01) |
| category_id | UUID | FK `asset_category.id` |
| status_id | UUID | FK `asset_status_def.id` |
| org_node_id | UUID | FK `org_node.id` — current physical location |
| department_org_node_id | UUID | FK `org_node.id`, nullable — cost-center/department scope, independent of location (FR-ORG-03) |
| parent_asset_id | UUID | FK self, nullable (FR-AST-04 bundles/accessories) |
| assigned_to_employee_id | UUID | FK `employee.id`, nullable — denormalized "current holder" (Decision D-1) |
| serial_number | VARCHAR(150) | indexed, nullable |
| manufacturer | VARCHAR(150) | |
| model | VARCHAR(150) | |
| description | TEXT | |
| barcode_value | VARCHAR(100) | unique (FR-AST-02) |
| qr_payload | VARCHAR(500) | unique |
| rfid_tag_id | VARCHAR(100) | unique, nullable — extension point (FR-AST-12), unused until R3 hardware |
| condition_code | VARCHAR(30) | last known condition, denormalized from latest audit finding |
| custom_attributes | JSONB | FR-AST-06, see Section 4 |
| vendor_id | UUID | FK `vendor.id`, nullable |
| purchase_order_id | UUID | FK `purchase_order.id`, nullable — R2 |
| purchase_date | DATE | nullable |
| transaction_currency_code | CHAR(3) | ISO 4217 (FR-AST-09) |
| purchase_cost_original | NUMERIC(14,2) | in `transaction_currency_code` |
| fx_rate_to_reporting | NUMERIC(18,8) | captured at entry time (FR-INV-10) |
| fx_rate_as_of_date | DATE | |
| purchase_cost_reporting | NUMERIC(14,2) | computed at entry time, never recomputed later |
| warranty_start_date | DATE | nullable |
| warranty_end_date | DATE | nullable, indexed (FR-NTF-01 trigger, R2) |
| amc_start_date | DATE | nullable |
| amc_end_date | DATE | nullable, indexed |
| legal_hold | BOOLEAN | default false (FR-CMP-06) |

Relationships: FKs to `asset_category`, `asset_status_def`, `org_node` (x2), `employee`, `vendor`, `purchase_order`, self (`parent_asset_id`). 1:1 with `asset_insurance_detail` and `vehicle_detail` (R2). 1:many with `asset_attachment`, `asset_history_event`, `asset_depreciation_schedule`. Audit columns: full, `@Version` optimistic locking (Section 8).

Indexes: unique on `asset_number`, `barcode_value`, `qr_payload`; B-tree on `(org_node_id)`, `(status_id)`, `(category_id)`, `(assigned_to_employee_id)`; GIN on `custom_attributes` (Section 4); partial index on `(warranty_end_date) WHERE warranty_end_date IS NOT NULL`.

### `asset_attachment` (R1)
Purpose: images/files per asset (FR-AST-05), object-store reference only (Section 10 file storage).

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_id | UUID | FK `asset.id` |
| attachment_type | VARCHAR(20) | `PHOTO`, `INVOICE`, `MANUAL`, `WARRANTY_CARD`, `OTHER` |
| object_key | VARCHAR(500) | MinIO object key, never a filesystem path |
| content_hash | VARCHAR(64) | SHA-256, for integrity verification |
| content_type | VARCHAR(100) | MIME type |
| size_bytes | BIGINT | |
| uploaded_by | UUID | |
| uploaded_at | TIMESTAMPTZ | |

Audit columns: `created_by`/`created_at` only (append-only; attachments are superseded by adding a new one, never edited in place).

### `asset_history_event` (R1)
Purpose: append-only history — see Decision D-5 above. Covers FR-AST-10, FR-AST-11 (via `event_type = LOCATION_CHANGE`), and FR-LIF-10 (via events raised by the Lifecycle module and persisted here through a domain-event listener).

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_id | UUID | FK `asset.id` |
| event_type | VARCHAR(30) | `STATUS_CHANGE`, `LOCATION_CHANGE`, `ASSIGNMENT_CHANGE`, `CONDITION_CHANGE`, `FIELD_UPDATE`, `LIFECYCLE_EVENT`, `CORRECTION` |
| field_name | VARCHAR(100) | nullable |
| old_value | TEXT | nullable |
| new_value | TEXT | nullable |
| from_org_node_id | UUID | FK `org_node.id`, nullable, populated for `LOCATION_CHANGE` |
| to_org_node_id | UUID | FK `org_node.id`, nullable |
| source_module | VARCHAR(30) | which module raised this (`AST`, `LIF`, `AUD`) |
| correction_of_event_id | UUID | FK self, nullable — corrections are new rows, never edits (Section 8) |
| recorded_by | UUID | |
| recorded_at | TIMESTAMPTZ | indexed together with `asset_id` |

Indexes: composite `(asset_id, recorded_at DESC)` — this is the table NFR-PERF-05's 1-second bounded-history-query target governs directly.

Audit columns: `created_by`(=`recorded_by`)/`created_at`(=`recorded_at`) only — strictly append-only.

### `asset_insurance_detail` (R2)
Purpose: dedicated, indexed insurance columns per Decision #6 / FR-AST-13 — contrasted with JSONB custom fields in Section 4.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_id | UUID | FK `asset.id`, unique per active policy (partial unique index `WHERE is_active`) |
| insurer_name | VARCHAR(200) | |
| policy_number | VARCHAR(100) | indexed |
| coverage_amount | NUMERIC(14,2) | |
| coverage_currency | CHAR(3) | |
| policy_start_date | DATE | |
| policy_expiry_date | DATE | **indexed** — FR-NTF-01 expiry alert trigger, FR-RPT-05 report filter |
| is_active | BOOLEAN | |

Audit columns: full.

### `asset_insurance_claim` (R2)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| insurance_detail_id | UUID | FK `asset_insurance_detail.id` |
| audit_finding_id | UUID | FK `audit_finding.id`, nullable — links claim to the damage/loss finding that triggered it (FR-AST-13) |
| claim_number | VARCHAR(100) | |
| filed_date | DATE | |
| claim_amount | NUMERIC(14,2) | |
| status | VARCHAR(20) | `FILED`, `APPROVED`, `DENIED`, `PAID`, `CLOSED` |
| resolved_date | DATE | nullable |

Audit columns: full.

### `vehicle_detail` (R2)
Purpose: dedicated, indexed Vehicle subtype columns per Decision #6 / FR-AST-14.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_id | UUID | FK `asset.id`, unique |
| vin | VARCHAR(17) | unique, indexed |
| registration_number | VARCHAR(20) | indexed |
| odometer_reading | INTEGER | |
| odometer_unit | VARCHAR(3) | `MI`, `KM` |
| registration_expiry_date | DATE | **indexed** — notification trigger |
| insurance_expiry_date | DATE | **indexed** — notification trigger |

Audit columns: full.

### `asset_depreciation_schedule` / `asset_depreciation_entry` (R2)
Purpose: FR-AST-15/FR-CMP-07 depreciation calculation (GAAP/IFRS-aligned methods).

`asset_depreciation_schedule`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_id | UUID | FK `asset.id`, unique |
| method | VARCHAR(30) | `STRAIGHT_LINE`, `DECLINING_BALANCE` |
| useful_life_months | INTEGER | |
| salvage_value_reporting | NUMERIC(14,2) | |
| depreciation_start_date | DATE | |
| accumulated_depreciation_reporting | NUMERIC(14,2) | denormalized running total, recomputed by the depreciation job |
| net_book_value_reporting | NUMERIC(14,2) | denormalized |
| last_computed_at | TIMESTAMPTZ | |

`asset_depreciation_entry` (append-only)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| schedule_id | UUID | FK `asset_depreciation_schedule.id` |
| period_start | DATE | |
| period_end | DATE | |
| depreciation_amount_reporting | NUMERIC(14,2) | |
| accumulated_after_reporting | NUMERIC(14,2) | |
| computed_at | TIMESTAMPTZ | |

Audit columns: schedule — full with `@Version`; entry — append-only.

## 2.4 Asset Lifecycle Management (LIF) — all R2 unless noted

### `purchase_request` (R2, FR-LIF-01)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| requested_by | UUID | |
| org_node_id | UUID | FK `org_node.id` |
| justification | TEXT | |
| status | VARCHAR(20) | `DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED` |
| approved_by | UUID | nullable |
| approved_at | TIMESTAMPTZ | nullable |

Audit columns: full.

### `vendor` (R2, FR-INV-07 / FR-AST-09)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| name | VARCHAR(200) | |
| tax_id | VARCHAR(50) | nullable |
| contact_name | VARCHAR(150) | nullable |
| contact_email | VARCHAR(255) | nullable |
| contact_phone | VARCHAR(30) | nullable |
| address | JSONB | freeform address structure — not personal data (organization, not individual) |
| is_active | BOOLEAN | |

Audit columns: full.

### `purchase_order` / `purchase_order_line` (R2, FR-LIF-02)

`purchase_order`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| po_number | VARCHAR(50) | unique |
| vendor_id | UUID | FK `vendor.id` |
| purchase_request_id | UUID | FK `purchase_request.id`, nullable |
| status | VARCHAR(20) | `ISSUED`, `PARTIALLY_RECEIVED`, `RECEIVED`, `CLOSED`, `CANCELLED` |
| order_date | DATE | |
| expected_delivery_date | DATE | nullable |
| transaction_currency_code | CHAR(3) | |
| total_amount_original | NUMERIC(14,2) | |
| fx_rate_to_reporting | NUMERIC(18,8) | |
| fx_rate_as_of_date | DATE | |
| total_amount_reporting | NUMERIC(14,2) | |

`purchase_order_line`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| purchase_order_id | UUID | FK |
| description | VARCHAR(300) | |
| quantity | NUMERIC(12,3) | |
| unit_cost_original | NUMERIC(14,2) | |
| category_id | UUID | FK `asset_category.id`, nullable |
| is_asset_line | BOOLEAN | true if this line becomes individually-tracked assets vs. inventory stock |

Audit columns: both full.

### `receiving_record` / `receiving_line` (R2, FR-LIF-03)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| purchase_order_id | UUID | FK `purchase_order.id` |
| received_by | UUID | |
| received_at | TIMESTAMPTZ | |
| reconciliation_status | VARCHAR(20) | `MATCHED`, `DISCREPANCY` |
| notes | TEXT | nullable |

`receiving_line`: `id`, `receiving_record_id` FK, `po_line_id` FK, `quantity_expected NUMERIC(12,3)`, `quantity_received NUMERIC(12,3)`, `discrepancy_reason TEXT` nullable.

Audit columns: both full.

### `asset_assignment` (R2, FR-LIF-04)
Purpose: append-only assignment history; `asset.assigned_to_employee_id` (R1, Decision D-1) is kept in sync transactionally by the service that inserts a new row here.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_id | UUID | FK `asset.id` |
| assignee_type | VARCHAR(20) | `EMPLOYEE`, `DEPARTMENT`, `ROOM` |
| assignee_employee_id | UUID | FK `employee.id`, nullable |
| assignee_org_node_id | UUID | FK `org_node.id`, nullable |
| assigned_by | UUID | |
| assigned_at | TIMESTAMPTZ | |
| acknowledged_at | TIMESTAMPTZ | nullable — Employee/Volunteer acknowledgment |
| ended_at | TIMESTAMPTZ | nullable — set when superseded by the next assignment |
| status | VARCHAR(20) | `ACTIVE`, `RETURNED`, `SUPERSEDED` |

Audit columns: `created_by`/`created_at` only (append-only; a status flip to `SUPERSEDED`/`RETURNED` is the one permitted mutation on the *current* row, done in the same transaction that inserts the successor row).

### `asset_transfer_request` (R2, FR-LIF-05, SoD-relevant)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_id | UUID | FK `asset.id` |
| from_org_node_id | UUID | FK `org_node.id` |
| to_org_node_id | UUID | FK `org_node.id` |
| from_employee_id | UUID | FK `employee.id`, nullable |
| to_employee_id | UUID | FK `employee.id`, nullable |
| initiated_by | UUID | — SoD actor field (Section 5) |
| initiated_at | TIMESTAMPTZ | |
| status | VARCHAR(20) | `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `COMPLETED` |
| approver_id | UUID | nullable — must differ from `initiated_by` unless `sod_waiver_id` set |
| approved_at | TIMESTAMPTZ | nullable |
| rejection_reason | TEXT | nullable |
| sod_waiver_id | UUID | FK `sod_waiver.id`, nullable |

Audit columns: full, `@Version`.

### `maintenance_event` / `maintenance_schedule` (R2, FR-LIF-06/07/08)

`maintenance_event`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_id | UUID | FK `asset.id` |
| maintenance_type | VARCHAR(20) | `REPAIR`, `PREVENTIVE`, `CORRECTIVE` |
| vendor_id | UUID | FK `vendor.id`, nullable |
| transaction_currency_code | CHAR(3) | |
| cost_original | NUMERIC(14,2) | |
| fx_rate_to_reporting | NUMERIC(18,8) | |
| fx_rate_as_of_date | DATE | |
| cost_reporting | NUMERIC(14,2) | |
| downtime_hours | NUMERIC(8,2) | nullable |
| scheduled_date | DATE | nullable |
| performed_date | DATE | nullable |
| triggered_by_audit_finding_id | UUID | FK `audit_finding.id`, nullable (FR-LIF-08) |
| status | VARCHAR(20) | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| notes | TEXT | nullable |

`maintenance_schedule`: `id`, `asset_id` FK, `recurrence_rule VARCHAR(200)` (RRULE string), `next_due_date DATE` indexed, `is_active BOOLEAN`.

Audit columns: both full.

### `asset_disposition` (R2, FR-LIF-09)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| asset_id | UUID | FK `asset.id` |
| disposition_type | VARCHAR(20) | `RETIREMENT`, `DISPOSAL`, `DONATION` |
| reason | VARCHAR(200) | |
| disposal_method | VARCHAR(30) | `RECYCLE`, `EWASTE_CERTIFIED`, `SALE`, `DONATION`, `LANDFILL`, `OTHER` |
| donation_recipient | VARCHAR(200) | nullable |
| disposed_by | UUID | |
| disposed_at | TIMESTAMPTZ | |
| transaction_currency_code | CHAR(3) | nullable |
| recovery_value_original | NUMERIC(14,2) | nullable |
| fx_rate_to_reporting | NUMERIC(18,8) | nullable |
| recovery_value_reporting | NUMERIC(14,2) | nullable |
| approval_status | VARCHAR(20) | `PENDING`, `APPROVED`, `REJECTED` (UC-LIF-02: Administrator approval required) |
| approved_by | UUID | nullable |
| approved_at | TIMESTAMPTZ | nullable |

Audit columns: full, `@Version`.

## 2.5 Audit Management (AUD) — all R2 (the core differentiator)

### `audit_definition` (R2, FR-AUD-01/02)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| audit_type | VARCHAR(20) | `QUARTERLY`, `ANNUAL`, `DEPARTMENT`, `ROOM`, `BUILDING`, `CAMPUS`, `SURPRISE`, `BULK` |
| name | VARCHAR(200) | |
| scope_org_node_id | UUID | FK `org_node.id` — root of the scope subtree |
| bulk_group_id | UUID | FK `audit_bulk_group.id`, nullable (FR-AUD-02) |
| sampling_method | VARCHAR(20) | `FULL`, `STATISTICAL_SAMPLE` (FR-AUD-20) |
| planned_start_date | DATE | |
| planned_end_date | DATE | |
| status | VARCHAR(20) | `DRAFT`, `IN_PROGRESS`, `SUBMITTED`, `PENDING_APPROVAL`, `APPROVED`, `CLOSED`, `REJECTED` |
| assigned_auditor_id | UUID | FK `app_user.id` |

Audit columns: full, `@Version`.

### `audit_bulk_group` (R2, FR-AUD-02): `id`, `name VARCHAR(200)`, `description TEXT`. Audit columns: full.

### `sampling_configuration` (R2, FR-AUD-20)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| audit_id | UUID | FK `audit_definition.id`, unique |
| population_size | INTEGER | |
| sample_size | INTEGER | |
| confidence_level | NUMERIC(5,2) | e.g. 95.00 |
| margin_of_error | NUMERIC(5,2) | |
| method | VARCHAR(30) | e.g. `SIMPLE_RANDOM` |
| random_seed | BIGINT | for reproducibility (BO-2 like-for-like comparability, BRD 1.3.1) |

Audit columns: full.

### `audit_scope_item` (R2, FR-AUD-03)
Purpose: the expected-asset list, **frozen at audit creation** and never regenerated — the anchor for Section 6's scope-integrity mechanism.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| audit_id | UUID | FK `audit_definition.id` |
| asset_id | UUID | FK `asset.id` |
| expected_status_snapshot | VARCHAR(30) | copy of `asset.status_id` code at freeze time |
| expected_org_node_id_snapshot | UUID | copy of `asset.org_node_id` at freeze time |
| is_sample_selected | BOOLEAN | default true; false = in scope population but not in the drawn sample |
| added_at | TIMESTAMPTZ | |

Unique `(audit_id, asset_id)`. Audit columns: `created_at` only (frozen by definition — no `updated_at`).

### `audit_finding` (R2, FR-AUD-04/08/09/11 — append-only)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| audit_id | UUID | FK `audit_definition.id` |
| scope_item_id | UUID | FK `audit_scope_item.id` |
| asset_id | UUID | FK `asset.id` (denormalized for query convenience) |
| verification_status | VARCHAR(20) | `VERIFIED`, `MISSING`, `DAMAGED`, `SCOPE_CHANGED`, `EXCLUDED`, `PENDING` |
| condition_code | VARCHAR(30) | FK `condition_scale.code`, nullable |
| scanned_by | UUID | nullable (not populated for system-generated `MISSING` rows at closure) |
| scanned_at | TIMESTAMPTZ | nullable |
| scan_device_info | VARCHAR(200) | nullable |
| remarks | TEXT | nullable |
| correction_of_finding_id | UUID | FK self, nullable |

Indexes: composite `(audit_id, verification_status)`, `(asset_id, scanned_at DESC)`. Audit columns: `created_by`/`created_at` only.

### `condition_scale` (R2, FR-AUD-09): `id`, `code VARCHAR(30)` unique, `label VARCHAR(100)`, `sort_order INTEGER`, `is_default BOOLEAN`. Audit columns: full.

### `audit_evidence` (R2, FR-AUD-10)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| finding_id | UUID | FK `audit_finding.id` |
| object_key | VARCHAR(500) | |
| content_hash | VARCHAR(64) | |
| content_type | VARCHAR(100) | |
| size_bytes | BIGINT | |
| uploaded_by | UUID | |
| uploaded_at | TIMESTAMPTZ | |

Audit columns: `created_by`/`created_at` only.

### `audit_signature` (R2, FR-AUD-12): `id`, `audit_id` FK, `signer_user_id` FK, `signature_type VARCHAR(30)` (`AUDITOR_SUBMISSION`, `DEPARTMENT_HEAD_APPROVAL`, `SUPER_ADMIN_APPROVAL`, `ALTERNATE_APPROVER_APPROVAL`), `signed_at TIMESTAMPTZ`, `signature_method VARCHAR(30)`, `ip_address INET`. Audit columns: `created_by`/`created_at` only.

### `audit_approval` (R2, FR-AUD-13, SoD-critical — Section 5/6)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| audit_id | UUID | FK `audit_definition.id` |
| approver_id | UUID | — SoD-checked against `audit_definition.assigned_auditor_id` |
| approval_role | VARCHAR(30) | `DEPARTMENT_HEAD`, `SUPER_ADMIN`, `ALTERNATE_APPROVER` |
| decision | VARCHAR(20) | `APPROVED`, `REJECTED`, `CLARIFICATION_REQUESTED` |
| decision_at | TIMESTAMPTZ | |
| comments | TEXT | nullable |
| sod_waiver_id | UUID | FK `sod_waiver.id`, nullable — set only when routing occurred under FR-AUD-22's waiver path |

Audit columns: `created_by`/`created_at` only.

### `audit_completion_certificate` (R2, FR-AUD-14): `id`, `audit_id` FK unique, `certificate_number VARCHAR(50)` unique, `generated_at TIMESTAMPTZ`, `pdf_object_key VARCHAR(500)`. Audit columns: `created_at` only.

> **Note:** FR-AUD-15's Exception Report is intentionally *not* a stored entity — it is a query (`audit_finding WHERE verification_status IN ('MISSING','DAMAGED')`) rendered by the Reporting module. Persisting it separately would create a second, driftable copy of finding data.

### `audit_scope_change_event` (R2, FR-AUD-23) — see Section 6 for full mechanism design.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| audit_id | UUID | FK `audit_definition.id` |
| scope_item_id | UUID | FK `audit_scope_item.id` |
| asset_id | UUID | FK `asset.id` |
| triggering_event_type | VARCHAR(30) | `TRANSFER`, `STATUS_CHANGE`, `REASSIGNMENT`, `DISPOSITION` |
| triggering_reference_entity_type | VARCHAR(50) | e.g. `AssetTransferRequest` |
| triggering_reference_entity_id | UUID | polymorphic reference, not FK-enforced (source entity is owned by another module) |
| detected_at | TIMESTAMPTZ | |
| disposition_status | VARCHAR(30) | `PENDING_DISPOSITION`, `CONFIRMED_VERIFIED_NEW_LOCATION`, `EXCLUDED_FROM_SCOPE`, `ACCEPTED_AS_EXCEPTION` |
| dispositioned_by | UUID | nullable |
| dispositioned_at | TIMESTAMPTZ | nullable |
| disposition_notes | TEXT | nullable |

Audit columns: `created_at` + `dispositioned_by`/`dispositioned_at` in lieu of `updated_by`/`updated_at` (the one intended mutation is the disposition decision, modeled as explicit named columns rather than a generic update, so it is always clear whether a row has been actioned).

### `audit_reconciliation` (R2, FR-AUD-21)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| original_finding_id | UUID | FK `audit_finding.id` (the `MISSING` finding being resolved) |
| found_by | UUID | |
| found_at | TIMESTAMPTZ | |
| actual_org_node_id | UUID | FK `org_node.id` |
| condition_update_code | VARCHAR(30) | nullable |
| linked_new_finding_id | UUID | FK `audit_finding.id`, nullable |
| notes | TEXT | nullable |

Audit columns: `created_by`/`created_at` only.

## 2.6 Barcode/QR/RFID Scanning (SCN)

### `label_template` (R1, FR-SCN-07): `id`, `name VARCHAR(150)`, `symbology VARCHAR(10)` (`CODE128`, `QR`), `size_code VARCHAR(20)`, `layout_config JSONB`, `is_default BOOLEAN`. Audit columns: full.

### `scan_event` (R2, FR-SCN-01–06, append-only)
Purpose: every resolved (and unresolved) scan during an audit session; the offline-sync/dedup anchor.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| client_generated_id | UUID | **unique** — generated on-device, including offline; sync dedups on this, not `id` |
| audit_id | UUID | FK `audit_definition.id`, nullable (a scan can happen outside an active audit, e.g. FR-SRC-02 lookup) |
| scanned_code | VARCHAR(300) | raw decoded value |
| resolved_asset_id | UUID | FK `asset.id`, nullable |
| scan_source | VARCHAR(20) | `USB_HID`, `BLUETOOTH_HID`, `CAMERA`, `RFID` |
| device_info | VARCHAR(200) | nullable |
| is_duplicate | BOOLEAN | FR-SCN-04 |
| is_unrecognized | BOOLEAN | true when `resolved_asset_id` is null |
| scanned_by | UUID | |
| scanned_at | TIMESTAMPTZ | client-side timestamp, may predate server receipt when synced from offline queue |
| synced_from_offline | BOOLEAN | |
| received_at | TIMESTAMPTZ | server receipt time |

Audit columns: `created_by`(=`scanned_by`)/`created_at`(=`received_at`) only.

## 2.7 Reporting (RPT)

### `report_definition` (R1 minimal catalog; full catalog R2): `id`, `code VARCHAR(50)` unique, `name VARCHAR(150)`, `category VARCHAR(50)`, `required_permission_code VARCHAR(100)`, `is_active BOOLEAN`. Audit columns: full.

### `report_export_job` (R1 — needed for NFR-PERF-04 background export even in R1's basic reporting scope)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| report_definition_id | UUID | FK |
| requested_by | UUID | |
| requested_at | TIMESTAMPTZ | |
| filters | JSONB | |
| format | VARCHAR(10) | `PDF`, `EXCEL`, `CSV` |
| status | VARCHAR(20) | `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED` |
| result_object_key | VARCHAR(500) | nullable |
| completed_at | TIMESTAMPTZ | nullable |

Audit columns: `created_by`/`created_at` only.

### `report_schedule_subscription` (R2, FR-RPT-13): `id`, `report_definition_id` FK, `filters JSONB`, `schedule_cron VARCHAR(100)`, `recipient_user_ids JSONB` (array), `format VARCHAR(10)`, `created_by UUID`, `is_active BOOLEAN`. Audit columns: full.

## 2.8 Dashboard (DSH)

### `dashboard_widget_preference` (R2, FR-DSH-06): `id`, `user_id` FK, `widget_code VARCHAR(50)`, `position INTEGER`, `config JSONB`. Audit columns: full.

> Dashboards themselves (FR-DSH-01–05/07) are computed read-models over AST/AUD/INV/LIF tables, refreshed reactively via domain events (Section 9), not separately persisted aggregate tables — this avoids a second source of truth for numbers that must match Reporting exactly.

## 2.9 User Management & RBAC (USR) — all R1

### `app_user`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| username | VARCHAR(100) | unique |
| email | VARCHAR(255) | `@PersonalDataField(category=CONTACT)`, unique |
| password_hash | VARCHAR(200) | nullable (null when `auth_source != LOCAL`); bcrypt/Argon2 |
| auth_source | VARCHAR(10) | `LOCAL`, `LDAP`, `SSO` |
| external_directory_id | VARCHAR(200) | nullable |
| employee_id | UUID | FK `employee.id`, nullable |
| is_active | BOOLEAN | |
| mfa_enabled | BOOLEAN | |
| mfa_secret_encrypted | VARCHAR(500) | nullable, encrypted at rest |
| failed_login_count | INTEGER | default 0 |
| locked_until | TIMESTAMPTZ | nullable |
| last_login_at | TIMESTAMPTZ | nullable |

Audit columns: full, `@Version`.

### `role`: `id`, `code VARCHAR(50)` unique, `name VARCHAR(100)`, `description TEXT`, `is_system_default BOOLEAN`. Audit columns: full. (FR-USR-01/02; roles **do not** inherit from one another — Section 5.)

### `permission`: `id`, `code VARCHAR(100)` unique (e.g. `AUDIT_APPROVE`, `ASSET_WRITE`), `description TEXT`, `module_code VARCHAR(10)`. Audit columns: full.

### `role_permission` (join table): `role_id` FK, `permission_id` FK — composite PK `(role_id, permission_id)`. No audit columns (pure join table); changes tracked via `security_activity_log`.

### `user_role_assignment` (FR-USR-04 — org-scoped, multi-role)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK `app_user.id` |
| role_id | UUID | FK `role.id` |
| org_scope_node_id | UUID | FK `org_node.id`, nullable (null = entire organization) |
| granted_by | UUID | |
| granted_at | TIMESTAMPTZ | |
| revoked_at | TIMESTAMPTZ | nullable |

Audit columns: `created_by`/`created_at` + explicit `revoked_at` (append-style: a revoked assignment is closed out, not deleted, so historical "who could do what, when" remains reconstructable for the security log).

### `password_history`: `id`, `user_id` FK, `password_hash VARCHAR(200)`, `changed_at TIMESTAMPTZ`. Audit columns: `created_at` only.

### `refresh_token` (FR-SEC-01)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK `app_user.id` |
| token_hash | VARCHAR(200) | never store the raw token |
| issued_at | TIMESTAMPTZ | |
| expires_at | TIMESTAMPTZ | |
| revoked_at | TIMESTAMPTZ | nullable — enables immediate server-side revocation |
| device_info | VARCHAR(200) | nullable |

Audit columns: `created_at` only.

### `sod_waiver` (FR-USR-07) — see Section 5.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| action_type | VARCHAR(50) | e.g. `AUDIT_APPROVAL`, `TRANSFER_APPROVAL`, `VALUATION_APPROVAL` |
| scope_org_node_id | UUID | FK `org_node.id`, nullable (null = organization-wide) |
| reason | TEXT | |
| approved_by | UUID | FK `app_user.id` |
| approved_at | TIMESTAMPTZ | |
| it_security_officer_id | UUID | FK `app_user.id`, nullable until signed off |
| it_security_officer_signoff | BOOLEAN | default false — waiver is not effective until true |
| effective_from | TIMESTAMPTZ | |
| effective_until | TIMESTAMPTZ | nullable (open-ended) |
| is_active | BOOLEAN | |

Audit columns: full, `@Version`.

### `offboarding_case` / `offboarding_asset_action` (FR-USR-05)

`offboarding_case`: `id`, `employee_id` FK, `initiated_by UUID`, `initiated_at TIMESTAMPTZ`, `status VARCHAR(20)` (`PENDING_ASSET_RESOLUTION`, `COMPLETED`), `completed_at TIMESTAMPTZ` nullable, `blocking_asset_count INTEGER`.

`offboarding_asset_action`: `id`, `offboarding_case_id` FK, `asset_id` FK, `action_type VARCHAR(30)` (`RETURNED_TO_INVENTORY`, `REASSIGNED`), `target_employee_id` FK nullable, `performed_by UUID`, `performed_at TIMESTAMPTZ`.

Audit columns: both full (case) / append-only (action).

## 2.10 Notifications (NTF) — all R2

### `notification_event_type`: `id`, `code VARCHAR(50)` unique, `name VARCHAR(150)`, `is_mandatory_configurable BOOLEAN`, `default_channels JSONB`. Audit columns: full.

### `notification_preference` (FR-NTF-05): `id`, `user_id` FK, `event_type_id` FK, `channel VARCHAR(10)` (`EMAIL`, `IN_APP`, `BOTH`, `NONE`), `is_locked_by_admin BOOLEAN`. Unique `(user_id, event_type_id)`. Audit columns: full.

### `notification` (FR-NTF-03/04)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| recipient_user_id | UUID | FK `app_user.id` |
| event_type_id | UUID | FK `notification_event_type.id` |
| title | VARCHAR(300) | |
| body | TEXT | |
| related_entity_type | VARCHAR(100) | nullable |
| related_entity_id | UUID | nullable |
| is_read | BOOLEAN | default false |
| read_at | TIMESTAMPTZ | nullable |

Audit columns: `created_at` only.

### `notification_delivery_log` (append-only): `id`, `notification_id` FK, `channel VARCHAR(10)`, `status VARCHAR(20)` (`SENT`, `FAILED`, `SKIPPED_PREFERENCE`), `attempted_at TIMESTAMPTZ`, `error_message TEXT` nullable.

## 2.11 Search (SRC)

### `saved_search` (R2, FR-SRC-04): `id`, `user_id` FK, `name VARCHAR(150)`, `entity_type VARCHAR(50)`, `filter_criteria JSONB`, `created_at TIMESTAMPTZ`.

> No dedicated search-index entity — see Section 12.2 rationale for using PostgreSQL `pg_trgm`/`tsvector` GIN indexes directly on `asset`, `employee`, `vendor`, `purchase_order` rather than standing up a separate search engine.

## 2.12 Security (SEC) — all R1

### `security_activity_log` (append-only, FR-SEC-04)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| event_type | VARCHAR(40) | `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `LOGOUT`, `PERMISSION_DENIED`, `ADMIN_ACTION`, `DATA_EXPORT`, `PASSWORD_CHANGE`, `MFA_CHALLENGE`, `SOD_WAIVER_ACTION` |
| actor_user_id | UUID | FK `app_user.id`, nullable (pre-auth events) |
| target_entity_type | VARCHAR(100) | nullable |
| target_entity_id | UUID | nullable |
| ip_address | INET | |
| user_agent | VARCHAR(500) | nullable |
| detail | JSONB | event-specific structured detail |
| occurred_at | TIMESTAMPTZ | indexed |

Indexes: `(actor_user_id, occurred_at DESC)`, `(event_type, occurred_at DESC)`.

### `ip_access_restriction` (FR-SEC-07): `id`, `cidr_range VARCHAR(50)`, `description VARCHAR(200)`, `is_allow_list BOOLEAN`, `is_active BOOLEAN`. Audit columns: full.

### `step_up_auth_challenge` (FR-SEC-06): `id`, `user_id` FK, `action_type VARCHAR(50)`, `challenged_at TIMESTAMPTZ`, `verified_at TIMESTAMPTZ` nullable, `method VARCHAR(20)`. Audit columns: `created_at` only.

> API rate limiting (NFR-API-01/02) is enforced at the reverse-proxy layer (Nginx/Traefik, SRS 2.5) and is configuration, not application data — no table.

## 2.13 Data Migration & Bulk Import/Export (MIG)

### `import_job` (R1, FR-MIG-01/03/04)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| entity_type | VARCHAR(30) | `ASSET`, `EMPLOYEE`, `VENDOR` |
| source_object_key | VARCHAR(500) | uploaded file, in object storage |
| mode | VARCHAR(10) | `DRY_RUN`, `COMMIT` |
| status | VARCHAR(20) | `QUEUED`, `VALIDATING`, `VALIDATED`, `COMMITTING`, `COMPLETED`, `FAILED` |
| total_rows | INTEGER | |
| valid_rows | INTEGER | |
| error_rows | INTEGER | |
| submitted_by | UUID | |
| submitted_at | TIMESTAMPTZ | |
| completed_at | TIMESTAMPTZ | nullable |

### `import_row_result` (R1)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| import_job_id | UUID | FK `import_job.id` |
| row_number | INTEGER | |
| raw_data | JSONB | |
| outcome | VARCHAR(20) | `CREATED`, `UPDATED`, `REJECTED`, `WOULD_CREATE`, `WOULD_UPDATE`, `WOULD_REJECT` |
| error_messages | JSONB | array of strings, nullable |
| created_entity_id | UUID | nullable |

Audit columns (both): `created_by`/`created_at` only (a job record is itself immutable once terminal; re-runs are new `import_job` rows).

### `export_job` (R3, FR-MIG-02): `id`, `entity_type VARCHAR(30)`, `filter_criteria JSONB`, `format VARCHAR(10)`, `status VARCHAR(20)`, `requested_by UUID`, `requested_at TIMESTAMPTZ`, `result_object_key VARCHAR(500)` nullable. Audit columns: `created_by`/`created_at` only.

## 2.14 External Integrations (INT)

### `integration_config` (R1 for LDAP/SSO governance — Decision D-4; extended in R3)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| integration_type | VARCHAR(30) | `LDAP`, `SSO_SAML`, `SSO_OIDC`, `ACCOUNTING_ERP`, `HR_SIS`, `SMS_GATEWAY`, `WEBHOOK` |
| is_enabled | BOOLEAN | default false (FR-INT-05) |
| config | JSONB | non-secret configuration (host, base DN, endpoint URL...) |
| secret_reference | VARCHAR(200) | pointer into secrets manager — **never** the secret itself (SRS 6.5) |
| enabled_by | UUID | nullable |
| enabled_at | TIMESTAMPTZ | nullable |
| compliance_signoff_by | UUID | nullable — Compliance Officer sign-off (BRD 6.5) |
| compliance_signoff_at | TIMESTAMPTZ | nullable |

Audit columns: full, `@Version`.

### `webhook_subscription` (R3, FR-INT-04): `id`, `integration_config_id` FK, `target_url VARCHAR(500)` (allow-listed, admin-set only), `event_types JSONB`, `signing_secret_reference VARCHAR(200)`, `is_active BOOLEAN`. Audit columns: full.

### `webhook_delivery_attempt` (R3, append-only): `id`, `webhook_subscription_id` FK, `event_type VARCHAR(100)`, `payload_hash VARCHAR(64)`, `http_status INTEGER` nullable, `attempted_at TIMESTAMPTZ`, `success BOOLEAN`, `response_snippet VARCHAR(500)` nullable.

### `integration_sync_log` (R3, FR-INT-02, append-only): `id`, `integration_config_id` FK, `sync_type VARCHAR(30)`, `started_at TIMESTAMPTZ`, `completed_at TIMESTAMPTZ` nullable, `records_processed INTEGER`, `records_failed INTEGER`, `status VARCHAR(20)`.

## 2.15 Compliance & Data Privacy (CMP) — all R1 (compliance baseline)

### `retention_policy` (FR-CMP-01): `id`, `entity_type VARCHAR(100)`, `retention_period_months INTEGER`, `action VARCHAR(20)` (`ANONYMIZE`, `DELETE`), `basis_description TEXT`, `configured_by UUID`, `configured_at TIMESTAMPTZ`. Audit columns: full.

### `legal_hold` (FR-CMP-06)
Purpose: generic polymorphic hold, works across any table via `entity_type`/`entity_id`, per the schema-level marker principle (SRS 4.1, Decision #11).

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| entity_type | VARCHAR(100) | |
| entity_id | UUID | polymorphic — not FK-enforced |
| reason | TEXT | |
| placed_by | UUID | |
| placed_at | TIMESTAMPTZ | |
| lifted_by | UUID | nullable |
| lifted_at | TIMESTAMPTZ | nullable |
| is_active | BOOLEAN | |

Audit columns: `created_by`/`created_at` + explicit `lifted_by`/`lifted_at`.

> **Decision D-6:** in addition to this generic `legal_hold` table (used for ad hoc holds on any entity type), high-traffic entities (`asset`, `audit_definition`) also carry their own denormalized `legal_hold BOOLEAN` column (Section 2.3/2.5) so the hot-path check ("is this specific asset held") is a single indexed column read, not a join to a polymorphic table on every request. The `legal_hold` table remains the system of record; the denormalized column is kept in sync by the domain service that places/lifts a hold, in the same transaction.

### `anonymization_job` (FR-CMP-02)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| entity_type | VARCHAR(100) | typically `Employee` |
| entity_id | UUID | |
| triggered_by | VARCHAR(20) | `POLICY_EXPIRY`, `MANUAL_REQUEST` |
| requested_by | UUID | nullable (system, for policy-expiry triggers) |
| requested_at | TIMESTAMPTZ | |
| approved_by | UUID | nullable — Compliance Officer approval (UC-CMP-01) |
| approved_at | TIMESTAMPTZ | nullable |
| status | VARCHAR(30) | `PENDING_APPROVAL`, `BLOCKED_LEGAL_HOLD`, `COMPLETED`, `REJECTED` |
| completed_at | TIMESTAMPTZ | nullable |

Audit columns: `created_by`/`created_at` + explicit approval columns (same rationale as `audit_scope_change_event`).

### `privacy_notice_config` (FR-CMP-03): `id`, `jurisdiction VARCHAR(50)`, `notice_text TEXT`, `data_categories JSONB`, `effective_date DATE`, `approved_by_compliance_officer UUID`. Audit columns: full.

## 2.16 Product Analytics (ANL)

### `feedback_submission` (R1 — Decision D-3, FR-ANL-04)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| submitted_by_user_id | UUID | FK `app_user.id` |
| category | VARCHAR(20) | `BUG`, `SUGGESTION`, `QUESTION` |
| message | TEXT | |
| submitted_at | TIMESTAMPTZ | |
| routed_to_user_id | UUID | FK `app_user.id` |
| status | VARCHAR(20) | `NEW`, `TRIAGED`, `CLOSED` |

Audit columns: `created_by`/`created_at` only.

### `usage_metric_daily` (R2 — Decision D-3, FR-ANL-01/02)
Purpose: pre-aggregated daily rollup — deliberately **not** a per-click event log, so no personal data or document content ever enters the metrics store (FR-ANL-01) and table growth is bounded (`~modules × features × roles × days`, not `~clicks`).

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| metric_date | DATE | |
| module_code | VARCHAR(10) | |
| feature_code | VARCHAR(100) | |
| role_code | VARCHAR(50) | |
| usage_count | INTEGER | |

Unique `(metric_date, module_code, feature_code, role_code)` — incremented via `INSERT ... ON CONFLICT (...) DO UPDATE SET usage_count = usage_count + 1`. Audit columns: none (pure metric rollup).

# 3. Organizational Hierarchy Schema

**Decision: adjacency list (`parent_id`) as the structural source of truth, with a PostgreSQL `ltree` `path` column as a derived, trigger-maintained secondary representation used for all read-path subtree/ancestor queries.**

## 3.1 Why not pure adjacency list + recursive CTE

A pure adjacency list is the simplest correct representation and is fine for occasional, shallow queries. It fails the scale target here specifically: FR-USR-04/ORG-05 require *every* asset, audit, and user-role-scope check to resolve "is node X within the subtree rooted at node Y" — and this happens on the hot path of nearly every authorization check (Section 6, SRS 2.4) and every scoped list query, at NFR-PERF-01's 500ms p95 target against 100,000+ assets. A recursive CTE re-walks the tree on every such call; at deep hierarchies (Campus > Building > Floor > Room, potentially 5+ levels, times many sites) this is measurably worse than a single indexed range scan, and it does not parallelize or cache well.

## 3.2 Why not a pure closure table

A closure table (`org_node_closure(ancestor_id, descendant_id, depth)`) gives O(1) subtree queries via an indexed join, and was seriously considered. It is rejected as the *sole* representation for two reasons: (1) it is `O(n²)` in the worst case (a balanced tree of depth `d` and fan-out `f` produces a closure table roughly `d/2` times larger than the node count — acceptable at this system's actual depth, but it duplicates data for no benefit `ltree` doesn't already give); (2) a node **move** (re-parenting a Building under a different Campus, an operation UC-ORG-01 explicitly supports) requires deleting and re-inserting every closure row for the entire moved subtree — expensive and easy to get wrong transactionally. `ltree` gives the same query performance with a single-column `UPDATE` per moved node (path relabeling), which is simpler to reason about correctness-wise.

## 3.3 The chosen representation

```sql
CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE org_node (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id     UUID REFERENCES org_node(id),
    node_type_id  UUID NOT NULL REFERENCES org_node_type(id),
    name          VARCHAR(200) NOT NULL,
    code          VARCHAR(50)  NOT NULL,
    path          LTREE NOT NULL,
    depth         INTEGER NOT NULL,
    -- ... other columns per Section 2.2
    UNIQUE (parent_id, code)
);

CREATE INDEX idx_org_node_path_gist ON org_node USING GIST (path);
CREATE INDEX idx_org_node_parent    ON org_node (parent_id);
```

- `parent_id` is the FK a human or the UI edits directly — it is what enforces referential integrity (`ON DELETE RESTRICT`, satisfying UC-ORG-01's "blocks deletion of a node with dependent assets" behavior transitively, since assets FK to `org_node`, not to `path`).
- `path` is maintained by a `BEFORE INSERT OR UPDATE OF parent_id` trigger that recomputes `path = parent.path || id::text::ltree` (root nodes: `path = id::text::ltree`) and cascades the recomputation to all descendants on a re-parent (a bounded `UPDATE ... WHERE path <@ old_path` — one statement, not N).
- Subtree scoping (the actual FR-ORG-05/FR-USR-04 query — "give me every asset under this Department Head's node") becomes: `SELECT * FROM asset a JOIN org_node n ON a.org_node_id = n.id WHERE n.path <@ (SELECT path FROM org_node WHERE id = :scopeNodeId)`, which uses the GiST index and is O(log n) in practice.
- Ancestor lookups (breadcrumb rendering, "does this node's chain include a Legal-Hold-flagged Campus") use `path @> :descendantPath` the same way.

This hybrid is deliberately *not* presented as "two sources of truth" — `path` is a derived column, recomputed only by the trigger, never written by application code, which keeps it consistent by construction rather than by application discipline.

# 4. Custom Fields vs. Dedicated Columns

Per SRS Section 4.1 and Decision #6 in the brief: an attribute is a first-class column **only** when it must be efficiently filterable/sortable at collection scale or is the trigger condition for a date-driven notification. Everything else — organization-specific, low-cardinality, not-bulk-queried — is JSONB.

## 4.1 JSONB design (FR-AST-06)

```sql
ALTER TABLE asset ADD COLUMN custom_attributes JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE INDEX idx_asset_custom_attributes_gin
    ON asset USING GIN (custom_attributes jsonb_path_ops);
```

- Shape: a flat JSON object keyed by `asset_custom_field_definition.field_key` (Section 2.3), e.g. `{"lab_hazard_class": "B", "loaner_pool": true}`. The API layer validates incoming values against `asset_custom_field_definition.data_type`/`enum_options` at the application layer (Bean Validation cannot validate dynamic JSONB shape, so a custom `@Validator` bean does this per category — Section 12) before persisting; the database itself does not constrain the JSON shape beyond being valid JSON, since the whole point of this mechanism is zero-migration extensibility per organization.
- `jsonb_path_ops` (not the default `jsonb_ops`) is chosen because the only query pattern this index needs to support is containment (`custom_attributes @> '{"lab_hazard_class": "B"}'`) for ad hoc filtering — `jsonb_path_ops` produces a smaller, faster index for that pattern and is the documented tradeoff for workloads that don't need the `?`/`?|`/`?&` key-existence operators.
- Explicitly **not** indexed for sorting or range queries (`ORDER BY custom_attributes->>'x'`) — that access pattern is exactly what Decision #6 says belongs in a dedicated column instead. If a custom field's usage pattern grows into "org needs to sort/filter this at scale," that is a signal to promote it to a real column via a normal Flyway migration + backfill, not to force JSONB to do a column's job.

## 4.2 Contrast with dedicated-column entities

`asset_insurance_detail.policy_expiry_date` and `vehicle_detail.registration_expiry_date` / `vehicle_detail.insurance_expiry_date` (Section 2.3) are B-tree indexed `DATE` columns specifically because:

1. **FR-NTF-01** requires a scheduled job to answer "which expiries fall within the next N days" across the *entire* asset collection, continuously, in the background — a JSONB scan (`custom_attributes->>'policy_expiry' <@ ...`) cannot use a B-tree range index the way a real `DATE` column can, and this query runs on a schedule against up to 100,000 rows.
2. **FR-RPT-05** (Insurance/Warranty/AMC Expiry reports with a configurable lookahead window) is a direct range filter + sort, the textbook case for a real column per the SRS 4.1 rule.
3. These fields are **cross-organization schema-stable** — every deployment that uses insurance tracking or vehicle assets needs the *same* fields (insurer, policy number, VIN...), unlike FR-AST-06 fields which are genuinely ad hoc per organization. A field that's stable across all deployments belongs in the schema, not in a per-deployment JSON blob.

# 5. Separation-of-Duties Enforcement Mechanism

## 5.1 Design

FR-USR-06 requires a server-side, default-on block: the actor approving an action may not be the same identity who created/registered/initiated the entity the action applies to. This is implemented as a single reusable mechanism, not per-endpoint hand-rolled checks, so it cannot be silently skipped when a new approval endpoint is added later.

```java
package com.iams.user.domain.sod;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnforceSeparationOfDuties {
    String actionType();                 // matches sod_waiver.action_type
    Class<? extends CreatorIdentityResolver<?>> creatorResolver();
}
```

```java
package com.iams.user.domain.sod;

public interface CreatorIdentityResolver<T> {
    UUID resolveCreatorId(T targetEntity);      // e.g. AuditDefinition -> assigned_auditor_id
    UUID resolveScopeOrgNodeId(T targetEntity);  // for waiver scope matching
}
```

```java
package com.iams.user.application.sod;

@Aspect
@Component
@RequiredArgsConstructor   // constructor injection only, Section 12.1
public class SeparationOfDutiesAspect {

    private final CurrentUserProvider currentUserProvider;
    private final SoDWaiverRepository waiverRepository;
    private final SecurityActivityLogService activityLogService;
    private final ApplicationContext applicationContext;

    @Around("@annotation(enforceSoD)")
    public Object enforce(ProceedingJoinPoint pjp, EnforceSeparationOfDuties enforceSoD) throws Throwable {
        Object targetEntity = pjp.getArgs()[0];          // convention: first arg is the entity/DTO carrying identity
        UUID actorId = currentUserProvider.getCurrentUserId();

        CreatorIdentityResolver<Object> resolver = applicationContext.getBean(enforceSoD.creatorResolver());
        UUID creatorId = resolver.resolveCreatorId(targetEntity);
        UUID scopeNodeId = resolver.resolveScopeOrgNodeId(targetEntity);

        if (!actorId.equals(creatorId)) {
            return pjp.proceed();   // no conflict — normal path, most calls end here
        }

        Optional<SoDWaiver> activeWaiver = waiverRepository
                .findActiveWaiver(enforceSoD.actionType(), scopeNodeId, Instant.now());

        if (activeWaiver.isEmpty()) {
            throw new SeparationOfDutiesViolationException(enforceSoD.actionType(), actorId);
        }

        Object result = pjp.proceed();
        activityLogService.logWaiverCoveredAction(enforceSoD.actionType(), actorId, activeWaiver.get().getId());
        return result;
    }
}
```

Applied, for example, on `AuditApprovalService.approve(AuditApprovalRequest request)`, `AssetTransferApprovalService.approve(...)`, and the valuation-approval path referenced in FR-USR-06's own example — the aspect is annotation-driven so adding SoD protection to a new approval endpoint is a one-line addition, not new plumbing.

### 5.1.1 Reconciliation with the Middleware/Infra Spec (IAMS-MIS-1.0) — superseded trigger mechanism

The Middleware/Infra spec (Section 3) independently arrived at a centralized `AccessPolicyEvaluator` composing RBAC → org-scope → SoD as three layers of a single `@PreAuthorize`-invoked check, rather than SoD as its own independently-triggered `@Aspect`. Both specs were written without visibility into each other; run as literally written, an approval method annotated with both `@PreAuthorize("@accessPolicy.can(...)")` and `@EnforceSeparationOfDuties(...)` would evaluate SoD twice, against two separate waiver lookups and two separate audit-log writes — unacceptable for a control this security-sensitive.

**Resolution: the standalone `SeparationOfDutiesAspect` and `@EnforceSeparationOfDuties` annotation above are dropped as an independent trigger.** `AccessPolicyEvaluator` (MIS Section 3.2) is the single authorization entry point and owns triggering, matching its stronger fail-closed argument (a `@PreAuthorize`-gated method cannot execute at all without the evaluator returning `true`, vs. an aspect that could in principle be omitted from a new method). What this section's design *does* contribute, and which `AccessPolicyEvaluator`'s "layer 3" step must reuse rather than reinvent: the `CreatorIdentityResolver<T>` interface and its per-entity-type implementations (`AuditDefinition -> assigned_auditor_id`, `AssetTransferRequest -> initiated_by`, etc.), and the `SoDWaiverRepository` / `SecurityActivityLogService` beans referenced above. Concretely, `AccessPolicyEvaluator.can(...)`'s SoD branch (MIS Section 3.2) resolves the appropriate `CreatorIdentityResolver` for `target`'s entity type (a Spring bean lookup keyed by target type, not the annotation-attribute-class-reference mechanism shown above, since there's no longer an annotation carrying that reference) and calls `resolveCreatorId`/`resolveScopeOrgNodeId` on it, then proceeds exactly as MIS Section 3.2's `can(...)` already describes. One evaluator, one waiver lookup, one log entry per request.

## 5.2 `SoDWaiver` (Section 2.9) as the auditable relaxation

A waiver is scoped by `action_type` + optional `scope_org_node_id`, requires `it_security_officer_signoff = true` before `is_active` evaluates true (enforced by a domain invariant in `SoDWaiverService`, not just a UI checkbox), and has an `effective_from`/`effective_until` window. `SoDWaiverRepository.findActiveWaiver(...)` checks all four conditions in one query.

## 5.3 Distinct logging of waiver-covered actions

Every action taken under an active waiver is logged via `security_activity_log` with `event_type = 'SOD_WAIVER_ACTION'` and `detail` JSONB containing `{"waiverId": ..., "actionType": ..., "actorId": ..., "creatorId": ...}` — distinct from the `event_type = 'ADMIN_ACTION'`/domain-specific logging a normal (non-waiver) approval produces. This lets Officer Reyes (IT Security Officer persona) filter the activity log specifically for waiver-covered activity during a review, per FR-USR-07's explicit requirement.

## 5.4 FR-AUD-22 — the audit self-approval special case

`AuditApprovalService.approve(...)` carries `@EnforceSeparationOfDuties(actionType = "AUDIT_APPROVAL", creatorResolver = AuditCreatorResolver.class)`, where `AuditCreatorResolver.resolveCreatorId()` returns `audit_definition.assigned_auditor_id`. The FR-AUD-22-specific twist — "route to Super Administrator or a configured alternate approver instead of blocking forever when Auditor and Department Head are the same person" — is handled one layer up, in `AuditApprovalRoutingService`, which runs *before* the aspect fires:

1. When an audit is submitted (`AuditDefinition.status = SUBMITTED`), `AuditApprovalRoutingService` resolves the scoped Department Head for `scope_org_node_id`.
2. If `departmentHeadUserId == assigned_auditor_id` **and** an active `AUDIT_APPROVAL`-scope waiver exists, the routing service creates the pending `audit_approval` row with `approval_role = 'SUPER_ADMIN'` (or the organization's configured `alternate_approver_user_id`, stored on `integration_config`-adjacent org settings) instead of `DEPARTMENT_HEAD`.
3. If no active waiver exists in that case, audit submission itself is blocked at the routing step with a clear error — the aspect never even gets a chance to reject a real approval attempt, because one was never created. This satisfies "rather than permanently blocking the audit from ever closing" (FR-AUD-22) — the system fails fast at submission time with an actionable message ("this organization needs a SoD waiver to close this audit"), not silently at approval time.

# 6. Audit Scope-Integrity Mechanism

## 6.1 Design overview

FR-AUD-23's requirement — a mid-audit transfer/reassignment/status change must not silently produce a false "Missing" — is implemented as a domain-event listener inside the Audit module that reacts to events raised by other modules, never by the Audit module reaching into `asset_transfer_request` or `asset_history_event` directly (cross-module DB access is prohibited, SRS 2.3).

```
LIF module: AssetTransferRequest approved
    -> publishes AssetTransferCompletedEvent(assetId, fromOrgNodeId, toOrgNodeId, transferId)

AST module: Asset status changed (any path)
    -> publishes AssetStatusChangedEvent(assetId, oldStatusCode, newStatusCode)

AUD module: @TransactionalEventListener(phase = AFTER_COMMIT)
    AuditScopeIntegrityListener.onAssetTransferCompleted(event)
    AuditScopeIntegrityListener.onAssetStatusChanged(event)
```

## 6.2 `AuditScopeIntegrityListener` logic

```java
@Component
@RequiredArgsConstructor
public class AuditScopeIntegrityListener {

    private final AuditScopeItemRepository scopeItemRepository;
    private final AuditScopeChangeEventRepository scopeChangeRepository;
    private final AuditFindingRepository findingRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssetTransferCompleted(AssetTransferCompletedEvent event) {
        handleScopeChange(event.getAssetId(), "TRANSFER", "AssetTransferRequest", event.getTransferId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssetStatusChanged(AssetStatusChangedEvent event) {
        handleScopeChange(event.getAssetId(), "STATUS_CHANGE", "Asset", event.getAssetId());
    }

    private void handleScopeChange(UUID assetId, String triggerType, String refType, UUID refId) {
        // Only relevant if the asset is in-scope on a still-OPEN audit
        List<AuditScopeItem> openScopeItems =
            scopeItemRepository.findByAssetIdAndAuditStatusIn(assetId, List.of("IN_PROGRESS", "SUBMITTED", "PENDING_APPROVAL"));

        for (AuditScopeItem item : openScopeItems) {
            scopeChangeRepository.save(new AuditScopeChangeEvent(item, triggerType, refType, refId));

            // Do NOT let the asset be silently marked MISSING at closure —
            // upsert a PENDING finding at SCOPE_CHANGED instead of leaving it unverified.
            findingRepository.upsertScopeChangedFinding(item.getAuditId(), item.getId(), assetId);
        }
    }
}
```

## 6.3 The linking entity and disposition workflow

`audit_scope_change_event` (Section 2.5) is the linking record: it references the frozen `audit_scope_item`, the asset, and the triggering event by polymorphic type+id (so it can point at an `AssetTransferRequest`, a plain status change, a reassignment, or a disposition without four separate FK columns). Its `disposition_status` state machine:

```
PENDING_DISPOSITION  ──(Auditor/Dept Head confirms new location)──►  CONFIRMED_VERIFIED_NEW_LOCATION
PENDING_DISPOSITION  ──(Auditor/Dept Head excludes from scope)────►  EXCLUDED_FROM_SCOPE
PENDING_DISPOSITION  ──(Auditor/Dept Head accepts as exception)───►  ACCEPTED_AS_EXCEPTION
```

The corresponding `audit_finding.verification_status` for that asset is set to `SCOPE_CHANGED` (not `MISSING`) the moment the triggering event is detected, and stays there until dispositioned.

## 6.4 Closure gate

`AuditClosureService.finalizeAudit(auditId)` runs a precondition check before allowing `status` to transition to `CLOSED`:

```sql
SELECT COUNT(*) FROM audit_scope_change_event
WHERE audit_id = :auditId AND disposition_status = 'PENDING_DISPOSITION';
```

A non-zero count blocks finalization with a listed set of the specific assets awaiting disposition — this is the concrete enforcement of "require the Auditor or Department Head to explicitly disposition it... before the audit can be finalized" (FR-AUD-23). Only once every `audit_scope_change_event` for the audit reaches a terminal disposition state does `FR-AUD-08`'s "classify unverified expected assets as Missing" logic run — and it explicitly excludes any `scope_item_id` that has an associated `audit_scope_change_event`, so a legitimately moved asset is never swept into `MISSING` by the default closure logic.

# 7. Multi-Currency Implementation

Per FR-INV-10 / decision #7, exactly one `ReportingCurrencyConfig` row exists per deployment (Section 2, singleton pattern enforced by a unique partial index `WHERE is_active` or simply a single-row table with a `CHECK (id = '00000000-0000-0000-0000-000000000001')` guard).

Every transaction-bearing entity that can be recorded in a foreign currency — `asset` (purchase), `purchase_order`, `maintenance_event`, `asset_disposition` (recovery value), `inventory_transaction` — carries the same four-column pattern, captured **once, at entry time, and never recomputed**:

| Column | Type | Meaning |
|---|---|---|
| `transaction_currency_code` | `CHAR(3)` | ISO 4217 code the transaction actually happened in |
| `*_amount_original` | `NUMERIC(14,2)` | amount in `transaction_currency_code` |
| `fx_rate_to_reporting` | `NUMERIC(18,8)` | rate captured at entry time |
| `fx_rate_as_of_date` | `DATE` | the date that rate was valid for |
| `*_amount_reporting` | `NUMERIC(14,2)` | `*_amount_original * fx_rate_to_reporting`, computed and stored at entry time |

**Enforcement of the "never recomputed later" rule:** `*_amount_reporting` is computed in the application service layer (`FxConversionService.convert(...)`) at the moment the transaction is created, and the column is populated directly — it is never a generated/computed column and no report query multiplies `original * <a rate looked up now>`. Every aggregate report (Asset Register totals, Depreciation, Inventory Valuation — FR-RPT-01/09, FR-INV-06) sums `*_amount_reporting` exclusively. A per-line report additionally displays `transaction_currency_code`, `*_amount_original`, and `fx_rate_to_reporting`/`fx_rate_as_of_date` for traceability, satisfying FR-INV-10's audit-traceability clause. `FxConversionService` is the single point that reads `reporting_currency_config` and an organization-supplied FX rate source (manual entry at transaction time in R1/R2; an optional rate-feed integration is out of scope and would be a future `INT` addition, not built here).

# 8. Concurrency Implementation

## 8.1 Optimistic locking (`@Version`) — entity edits

Per NFR-CONC-01, every entity representing a mutable "current state" record that is edited (not appended-to) carries `version BIGINT` mapped with JPA `@Version`: `asset`, `asset_category`, `asset_status_def`, `asset_custom_field_definition`, `asset_insurance_detail`, `vehicle_detail`, `asset_depreciation_schedule`, `org_node`, `employee`, `app_user`, `role`, `user_role_assignment` (on revoke), `sod_waiver`, `audit_definition`, `asset_transfer_request`, `asset_disposition`, `integration_config`, `retention_policy`. A `StaleObjectStateException`/`OptimisticLockException` surfaces to the API layer as `HTTP 409 Conflict` via `ApiExceptionHandler` (Section 12.5), with a message instructing the client to reload and retry — acceptable per NFR-CONC-01's own stated rationale (conflicting edits to the same asset/audit record are rare).

Append-only tables (`asset_history_event`, `audit_finding`, `audit_evidence`, `security_activity_log`, `outbox_event`, etc. — see the "audit columns" note on each entity above) carry **no** `version` column: there is nothing to conflict on, since rows are never updated, only inserted (and, for a small named set of tables like `audit_scope_change_event`/`anonymization_job`, transitioned through explicit named "disposition" columns rather than a general-purpose update).

## 8.2 Atomic row-level operations — inventory quantity mutations

Per NFR-CONC-02, `inventory_stock_level.quantity_on_hand` is **never** protected by `@Version`. Optimistic locking here would produce an unacceptable retry storm under real contention (multiple staff logging Stock In/Out against the same warehouse item concurrently, FR-INV-02/05/08). Instead, every quantity mutation is a single, conditional, atomic SQL statement executed directly (native query via Spring Data `@Modifying @Query`, not read-modify-write through the entity):

```java
package com.iams.inventory.infrastructure.persistence;

public interface InventoryStockLevelRepositoryCustom {
    int decrementIfSufficient(UUID stockLevelId, BigDecimal quantity);
    void increment(UUID stockLevelId, BigDecimal quantity);
}

@Repository
@RequiredArgsConstructor
class InventoryStockLevelRepositoryCustomImpl implements InventoryStockLevelRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int decrementIfSufficient(UUID stockLevelId, BigDecimal quantity) {
        // Returns 1 if the row was updated (sufficient stock), 0 if not — caller checks the row count,
        // never re-reads-then-writes.
        return jdbcTemplate.update("""
            UPDATE inventory_stock_level
               SET quantity_on_hand = quantity_on_hand - ?, updated_at = now()
             WHERE id = ? AND quantity_on_hand >= ?
            """, quantity, stockLevelId, quantity);
    }

    @Override
    public void increment(UUID stockLevelId, BigDecimal quantity) {
        jdbcTemplate.update("""
            UPDATE inventory_stock_level
               SET quantity_on_hand = quantity_on_hand + ?, updated_at = now()
             WHERE id = ?
            """, quantity, stockLevelId);
    }
}
```

**Stock Transfer (FR-INV-08, UC-INV-01) atomicity pattern**, in `InventoryTransferService.execute(...)`:

```java
@Transactional
public InventoryTransfer execute(TransferCommand cmd) {
    int rowsUpdated = stockLevelRepository.decrementIfSufficient(cmd.sourceStockLevelId(), cmd.quantity());
    if (rowsUpdated == 0) {
        throw new InsufficientStockException(cmd.sourceStockLevelId(), cmd.quantity());
    }
    stockLevelRepository.increment(cmd.destinationStockLevelId(), cmd.quantity());

    InventoryTransaction outTxn = inventoryTransactionRepository.save(InventoryTransaction.transferOut(cmd));
    InventoryTransaction inTxn  = inventoryTransactionRepository.save(InventoryTransaction.transferIn(cmd, outTxn.getId()));

    return transferRepository.save(InventoryTransfer.completed(cmd, outTxn.getId(), inTxn.getId()));
}
```

Both `UPDATE` statements execute inside a single database transaction (standard Spring `@Transactional`, `READ COMMITTED` isolation is sufficient because each statement is individually atomic and conditional — no phantom-read window exists between the decrement and the increment that could produce an inconsistent total, since the decrement's `WHERE quantity_on_hand >= ?` guard is evaluated by the database at the moment of the row-level write, not by the application against a previously-read value). If the decrement's guard fails (insufficient stock), the transaction throws before the increment ever runs, and Spring's default rollback-on-exception behavior ensures no partial transfer is persisted. This directly satisfies UC-INV-01's "decrements the source and increments the destination atomically" and its "blocks the transfer and shows the actual available quantity" exception flow (the 0-row-updated case reads current `quantity_on_hand` once more, only for the error message, not for the decision).

The same `decrementIfSufficient`/`increment` primitives back Stock In/Out (FR-INV-02, single-sided) and Adjustments (FR-INV-05, which additionally require a mandatory `reason` and `approved_by` recorded on the resulting `inventory_transaction` row).

# 9. Domain Events / Cross-Module Integration

## 9.1 Mechanism choice: in-process `ApplicationEventPublisher`, not a message broker

**Decision:** cross-module reactions use Spring's `ApplicationEventPublisher`/`@TransactionalEventListener`, backed by the `outbox_event` table (Section 2.1) for the subset of reactions that must survive a process crash or be retried (notification dispatch, webhook delivery). A message broker (Kafka/RabbitMQ) is explicitly **not** introduced.

Rationale: SRS 2.1 commits to a layered modular monolith specifically to keep on-premises operational complexity low for IT-generalist deployers (BRD 11.2, NFR-DEPLOY constraints) — a broker is another container, another failure mode, another thing to patch and back up, for a system that already runs all modules in one JVM and one transaction manager. In-process publish/subscribe gives the same decoupling *within a single deployable* that a broker gives *across* deployables, without the operational cost, and is consistent with "no module directly accesses another module's DB tables" (SRS 2.3) — publishing an event and letting the owning module's own listener persist to its own tables satisfies that rule exactly as well as a broker would, at zero extra infrastructure. If a future phase splits modules into separate services, the `outbox_event` table already gives that migration path a natural seam (an outbox relay becomes a broker producer with no domain-logic change) — see Section 9.4.

## 9.2 Event catalog (representative, not exhaustive)

| Event | Published by | Consumed by | Effect |
|---|---|---|---|
| `AssetRegisteredEvent` | AST | NTF, ANL, DSH (cache invalidation) | Welcome/registration notification (R2); usage metric increment (R2); dashboard asset-count cache bust |
| `AssetStatusChangedEvent` | AST | AUD (Section 6), NTF, DSH | Scope-integrity check; status-change notification; dashboard refresh |
| `AssetTransferCompletedEvent` | LIF | AST (history append, per Decision D-5), AUD (Section 6), NTF | `asset_history_event` row; scope-integrity check; transfer notification |
| `EmployeeOffboardingInitiatedEvent` | USR | LIF | Triggers the assigned-asset review gate (FR-USR-05) — LIF exposes `AssetReassignmentRequiredQuery`, USR blocks deactivation until it returns empty |
| `AuditClosedEvent` | AUD | DSH, RPT, NTF, ANL | Dashboard/report cache refresh; completion notification; usage metric |
| `AuditScopeChangeDetectedEvent` | internal to AUD (Section 6) | AUD listener itself | Not cross-module — kept for symmetry/testability of the listener |
| `InventoryLowStockDetectedEvent` | INV | NTF | Low-stock alert (FR-DSH-04, FR-NTF-01) |
| `LegalHoldPlacedEvent` / `LegalHoldLiftedEvent` | CMP | AST, AUD | Sync the denormalized `legal_hold` column (Decision D-6) |
| `AnonymizationCompletedEvent` | CMP | SEC | Immutable log entry confirming the anonymization occurred |
| `WebhookableEventOccurred` | any module (wraps `AuditClosedEvent`, `AssetStatusChangedEvent`, etc.) | INT | Enqueues an `outbox_event` row consumed by the webhook dispatcher (R3) |

## 9.3 Reliability pattern

- **Same-transaction, no external effect (e.g., updating a denormalized column):** plain `@EventListener`, synchronous, inside the publisher's transaction — if the listener fails, the whole transaction rolls back, which is correct (the denormalized column must never be inconsistent with its source of truth).
- **Cross-cutting but still local (e.g., `AuditScopeIntegrityListener`, Section 6):** `@TransactionalEventListener(phase = AFTER_COMMIT)` — only fires once the publisher's transaction has actually committed, so the listener never acts on a change that gets rolled back, and runs in its own transaction so a listener failure cannot roll back the original write.
- **External or must-survive-a-restart effects (email/SMS/webhook dispatch):** the `AFTER_COMMIT` listener does not call the external system directly — it writes an `outbox_event` row (same local transaction pattern as above) and a separate `@Scheduled` `OutboxDispatcher` component polls `outbox_event WHERE status = 'PENDING' AND next_attempt_at <= now()`, dispatches, and updates `status`/`attempt_count`/`next_attempt_at` with exponential backoff. This is what makes notification and webhook delivery at-least-once even across an application restart mid-dispatch.

## 9.4 Extraction seam (not built now, just not precluded)

Because every cross-module reaction already goes through a named domain event and (for anything external-facing) the `outbox_event` table, extracting a module into a separate service later would mean swapping `ApplicationEventPublisher.publishEvent(...)` for a broker producer inside `DomainEventPublisher` (Section 1.3) — a single class — without touching any module's domain or application logic. This is noted as a design property, not a near-term plan; Section 9.1's decision stands as stated.

# 10. File Storage Architecture

Per SRS 4.2 / decision #10: `asset_attachment`, `audit_evidence`, `import_job.source_object_key`, `export_job.result_object_key`, `report_export_job.result_object_key`, and `audit_completion_certificate.pdf_object_key` all follow the same pattern — the database stores `object_key`/`content_hash`/`content_type`/`size_bytes` only, never a BLOB, never a bare filesystem path.

```java
package com.iams.common.storage;

public interface ObjectStoragePort {
    StoredObjectRef put(InputStream content, String contentType, long sizeBytes, String suggestedKeyPrefix);
    InputStream get(String objectKey);
    void delete(String objectKey);
}
```

The MinIO-backed implementation lives in `com.iams.common.infrastructure.storage.MinioObjectStorageAdapter` and is the **only** class in the codebase holding a MinIO client. Controllers never return a presigned MinIO URL to the browser — every file read is proxied: `GET /api/v1/assets/{id}/attachments/{attachmentId}/content` streams through the backend (`ObjectStoragePort.get(...)` piped to the `HttpServletResponse`), which is what SRS 4.2/6.7 mean by "the object store shall not be directly reachable by end-user clients" and "served back to clients through the backend rather than directly." Uploads are validated for content-type and size (FR-SEC-08/6.7) in the `api` layer before ever reaching `ObjectStoragePort.put(...)`.

# 11. Personal Data Tagging Mechanism

Per SRS 4.1 / decision #11, personal-data columns are tagged at the schema level via a field-level annotation, not a hardcoded table list, so FR-CMP-01/02 (retention + anonymization) operate generically:

```java
package com.iams.common.domain;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PersonalDataField {
    DataCategory category();     // NAME, CONTACT, GOVERNMENT_ID, ...
    boolean anonymizable() default true;   // false for e.g. a field a legal hold must never touch
}
```

At application startup, a `PersonalDataRegistryInitializer` (`ApplicationRunner`) scans all JPA-managed entity classes via Hibernate's metamodel for fields carrying `@PersonalDataField`, and reconciles the result into `personal_data_field_registry` (Section 2.1) — inserting new rows for newly-tagged fields, and never deleting existing rows automatically (a field that stops being tagged is flagged for manual compliance review rather than silently dropped from the registry, since that registry is itself a compliance artifact).

`AnonymizationService.anonymize(entityType, entityId)` (backing `anonymization_job`, Section 2.15) is generic: it queries `personal_data_field_registry` for the given `entityType`, and for each registered column, sets it to a stable pseudonymous value (`"REDACTED-" || md5(entityId)` pattern for text fields) via a single dynamically-built `UPDATE` — it does not contain per-entity-type `if` branches. This is what lets FR-CMP-02's "without hardcoding table-specific logic" requirement (SRS 4.1) hold as new personal-data-bearing entities are added over time (e.g., if a future release adds a `Student` entity distinct from `Employee`, tagging its fields with `@PersonalDataField` is sufficient — no change to `AnonymizationService`). Historical rows that *reference* an anonymized `employee_id` (e.g., `asset_assignment.assignee_employee_id`) are left untouched — the FK still resolves, it just now resolves to a record with redacted name/contact fields, which is exactly "preserving the integrity of historical asset and audit records" (FR-CMP-02, UC-CMP-01).

# 12. Migration / Versioning Strategy

## 12.1 Flyway file naming convention

```
V<release>.<sequence>__<module>_<description>.sql
```

Examples:
```
V1.0001__core_extensions_and_baseline.sql        -- CREATE EXTENSION ltree, pgcrypto; common tables
V1.0002__org_create_org_node_type.sql
V1.0003__org_create_org_node.sql
V1.0004__usr_create_app_user.sql
V1.0005__usr_create_role_permission.sql
V1.0006__ast_create_asset_category.sql
V1.0007__ast_create_asset.sql
...
V2.0001__inv_create_warehouse.sql
V2.0002__aud_create_audit_definition.sql
...
V3.0001__int_create_webhook_subscription.sql
```

- The `<release>` major segment (`1`, `2`, `3`) is purely organizational — it groups migrations by the release that introduced them for human readability during review; Flyway itself treats the full `V<release>.<sequence>` string as one linear version number, so `V1.0007` still runs strictly before `V2.0001`. It is **not** a mechanism for skipping or conditionally applying migrations — every environment, regardless of which release it's currently running, applies the full linear history in order.
- `<sequence>` is a 4-digit, globally monotonic counter (not reset per module) — this avoids collisions when two modules' migrations are authored in parallel branches and merged.
- `<module>` is always the FRS module code (lowercase) so `git log`/`ls` on the migrations directory sorted by name roughly clusters by feature area, even though execution order is defined purely by the version number, not by filename grouping.

## 12.2 Baseline strategy

- A fresh deployment runs every migration from `V1.0001` — there is no separate "baseline" schema dump; Flyway's own migration history is the single source of truth for schema state (NFR-MAINT-02).
- `flyway.baselineOnMigrate` is used only for the one documented case of onboarding an *existing* pre-IAMS database an organization insists on reusing (rare, given IAMS provisions its own PostgreSQL container per SRS 2.5) — not for normal releases.
- Every migration is idempotent-safe to re-run against Staging from a Production snapshot (NFR-MAINT-05) — `CREATE TABLE IF NOT EXISTS` is avoided in favor of Flyway's own checksum-based "already applied" tracking being the single guard, so a migration script itself stays a plain, readable DDL statement.

## 12.3 Staging → Production promotion (NFR-MAINT-02/05)

1. Migration authored and reviewed alongside the code change that needs it, in the same PR.
2. CI applies it to an ephemeral Testcontainers PostgreSQL instance as part of the integration test suite (Section 13) — this is the first gate, and it fails fast on syntax/constraint errors.
3. On merge, the migration is applied to Staging automatically as part of the Staging deploy pipeline, against a production-equivalent data snapshot (NFR-MAINT-05).
4. A human (Administrator/IT Security Officer per the release's risk profile) verifies Staging, then triggers the Production migration — Flyway runs the same, already-tested SQL; there is never a hand-edited "production version" of a migration.
5. Every release ships a paired rollback note (not a Flyway "undo" migration, which is a paid feature and also risky for destructive DDL) — for additive migrations (the vast majority: new table, new nullable column) rollback is "redeploy the previous application version, leave the additive schema in place," which is safe by construction; for the rare destructive migration (dropping/renaming a column), the migration is split into two releases — release N stops using the column but doesn't drop it, release N+1 drops it — so rollback of release N is always safe.

## 12.4 Why not native PostgreSQL `ENUM` types

Every `VARCHAR + CHECK constraint` status/type field in Section 2 (`asset.status_id` is the one exception, modeled as a reference table specifically because FR-AST-07 requires it to be admin-configurable at runtime) is deliberately not a native Postgres `ENUM` type. Adding a value to a Postgres enum (`ALTER TYPE ... ADD VALUE`) cannot run inside the same transaction as other DDL/DML in older PostgreSQL behavior patterns applications commonly still guard against, and enum value removal/reordering is effectively unsupported without a full type rebuild — both are exactly the kind of migration friction NFR-MAINT-02's "managed exclusively through automated Flyway migrations" is trying to keep low-risk. A `CHECK` constraint is a one-line, ordinary, transactional `ALTER TABLE ... DROP CONSTRAINT / ADD CONSTRAINT` migration.

# 13. Testing Strategy

## 13.1 Layers

| Layer | Tooling | What it covers |
|---|---|---|
| Unit | JUnit 5, Mockito | Domain services, mappers (MapStruct-generated code is trusted, its *usage* is tested), the SoD aspect's routing logic with mocked repositories |
| Repository/Slice | `@DataJpaTest` + Testcontainers PostgreSQL | Every custom `@Query`/native query, especially the `ltree` subtree queries (Section 3) and the atomic inventory operations (Section 8) |
| Integration | `@SpringBootTest` + Testcontainers (PostgreSQL + MinIO) | Full use-case flows through the real API layer, real transactions, real object storage |
| Contract/API | Spring `MockMvc`/`WebTestClient` against OpenAPI-generated contract | Request/response shape stability |
| Architecture | ArchUnit | Module-boundary rule (Section 1.3), layering rule (controllers never depend on repositories directly, only through `application`), no field injection (Section 12.1 below) |
| End-to-end | Playwright (frontend-owned, out of this document's scope) | Full user journeys per IAMS-PUC-1.0 use cases |

Testcontainers, not H2/an in-memory substitute, is mandatory for any test touching `ltree`, JSONB GIN indexing, or the atomic `UPDATE ... WHERE quantity_on_hand >= ?` pattern — none of these behave identically on an in-memory database, and a passing H2 test would be a false signal for exactly the mechanisms this document cares most about getting right.

## 13.2 Mandatory integration-test coverage (non-negotiable, per NFR-MAINT-03 and the brief's explicit call-out)

1. **SoD enforcement (Section 5).** A test asserting the creator-is-actor case is rejected without a waiver, accepted (and distinctly logged) with an active waiver, and rejected again once the waiver's `effective_until` passes. A dedicated test for FR-AUD-22's routing-to-Super-Administrator path when Auditor and Department Head resolve to the same user.
2. **Atomic inventory operations (Section 8).** A concurrency test that fires N parallel `decrementIfSufficient` calls against a stock level with quantity for exactly `N-1` successful decrements, asserting exactly one caller receives the "insufficient stock" outcome and the final `quantity_on_hand` is never negative — this is the test that would catch a regression back to read-modify-write. Run with a real Testcontainers PostgreSQL under an actual thread pool, not mocked.
3. **Anonymization not corrupting historical totals (Section 11, FR-CMP-02, FRS Section 4).** A test that: registers an asset, assigns it to an employee, closes an audit referencing that assignment, computes an Asset Register / Employee Asset List total, anonymizes the employee, and re-computes the same totals — asserting the totals are byte-identical before and after, and that the historical `asset_assignment`/`audit_finding` rows still resolve (via FK) to the now-redacted employee row rather than erroring.
4. **Audit scope-integrity (Section 6).** A test that opens an audit, transfers an in-scope asset via the LIF module mid-audit, asserts an `audit_scope_change_event` row appears with `PENDING_DISPOSITION`, asserts audit closure is blocked while it's pending, dispositions it, and asserts closure then succeeds without the asset appearing as `MISSING`.
5. **Multi-currency traceability (Section 7).** A test asserting that an aggregate report total reflects the `*_amount_reporting` value stored at entry time even after `reporting_currency_config`'s configured currency or a later manually-entered rate changes — i.e., the report must not "re-price" historical transactions.

## 13.3 Coverage targets

Per NFR-MAINT-03, module-level coverage targets are set in the engineering backlog, not this document — but domain/application-layer logic in `audit`, `user` (SoD), `inventory` (atomic operations), and `compliance` is held to a stricter bar (line coverage target ≥ 85%, and 100% of the branches enumerated in Section 13.2) than read-mostly modules like `dashboard`/`reporting`, given those four are where a silent correctness bug has the highest business cost (BO-2, BR-21, BR-22, BRD Section 6).

# 14. Coding Standards

## 14.1 Dependency injection

Constructor injection only, via Lombok `@RequiredArgsConstructor` on `final` fields — no `@Autowired` field injection anywhere in the codebase. This is enforced by an ArchUnit rule (`NoFieldInjectionTest`), not just convention, because field injection is exactly the pattern that makes the module-boundary and SoD-aspect wiring in Sections 5/9 hard to unit-test in isolation.

## 14.2 SOLID application

- **Single Responsibility**: `application` services are one use case each (`AssetRegistrationService`, not a catch-all `AssetService` doing registration + search + reporting).
- **Open/Closed**: the SoD mechanism (Section 5) and the scan-source abstraction (FR-SCN-06, an `ScanSourceAdapter` interface with `UsbHidAdapter`/`BluetoothHidAdapter`/`CameraAdapter` implementations, ready for a future `RfidAdapter`) are the two clearest examples — new behavior is added via a new implementation/annotation usage, not by editing existing branching logic.
- **Liskov/Interface Segregation**: repository *ports* live in `domain` as narrow interfaces (`AssetRepository extends JpaRepository<Asset, UUID>` plus a hand-written `AssetRepositoryCustom` for the ltree/JSONB queries that don't fit Spring Data's derived-query vocabulary) — application services depend on the narrow interface, not on Hibernate/JPA specifics.
- **Dependency Inversion**: `application` depends on `domain` repository interfaces; `infrastructure.persistence` provides the implementation. `ObjectStoragePort` (Section 10) is the same pattern for MinIO.

## 14.3 DTO/entity separation

JPA entities never cross the `api` boundary in either direction. Every controller method takes a `*Request` DTO (JSR-380 `@Valid`-annotated) and returns a `*Response` DTO. **MapStruct** (`org.mapstruct:mapstruct` + annotation processor) generates the mapping code at compile time — hand-written mappers are only used for the handful of cases MapStruct can't express directly (e.g., resolving `AssetResponse.currentLocationPath` from `Asset.orgNodeId` via a lookup, done through a MapStruct `@Context`-qualified helper method, not a manual `if` chain in the controller).

## 14.4 Input validation

Bean Validation (JSR-380 / `jakarta.validation`) annotations on every request DTO (`@NotNull`, `@Size`, `@Pattern`, custom constraint annotations for domain rules like `@ValidCurrencyCode`). Per SRS 6.7, this is enforced server-side regardless of what the frontend does — validation failures produce a structured `400 Bad Request` via the same `@ControllerAdvice` below, never a raw stack trace. JSONB custom-field values (Section 4), which JSR-380 cannot validate structurally, go through a dedicated `CustomAttributesValidator` domain service invoked explicitly by the relevant `application` service before persistence.

## 14.5 Centralized exception handling

```java
package com.iams.common.web;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) { ... }   // 400

    @ExceptionHandler(SeparationOfDutiesViolationException.class)
    ResponseEntity<ApiError> handleSoD(SeparationOfDutiesViolationException ex) { ... }      // 403

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleConflict(OptimisticLockingFailureException ex) { ... }     // 409

    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex) { ... }   // 409

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex) { ... }               // 404

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) { ... }             // 403
}
```

Every response body is the shared `ApiError` shape (`code`, `message`, `details`, `correlationId` — the last populated from the MDC correlation ID NFR-OBS-03 requires on every request), so a frontend error handler never branches on exception-specific shapes.

## 14.6 Pagination

`Pageable` is a mandatory parameter on every list endpoint (`@PageableDefault(size = 50, sort = "createdAt", direction = DESC)`), per NFR-SCALE-03 — there is no "return everything" list endpoint anywhere in the API surface; an ArchUnit rule flags any `@GetMapping` controller method returning a bare `List<T>`/`Collection<T>` for a paginated-entity type as a build failure. Responses use the shared `PageResponse<T>` envelope (`content`, `page`, `size`, `totalElements`, `totalPages`) wrapping Spring Data's `Page<T>`, so pagination metadata shape is identical across all 16 modules.

# 15. Open Items Flagged for Later Resolution

This document resolves every ambiguity the brief called out explicitly. The following are genuinely open and are flagged here rather than silently decided, because they need either a stakeholder decision or downstream-artifact detail this document doesn't own:

1. **FX rate source (Section 7).** This document specifies where the rate is stored and how it's used, not where it comes from. R1/R2 assume manual entry at transaction time; whether a future release adds an automated rate-feed integration is a product decision for module INT, not a data-model one.
2. **`alternate_approver_user_id` configuration surface (Section 5.4).** FR-AUD-22 requires a "configured alternate approver" — this document assumes it is a simple per-organization setting but does not define whether it is global or org-node-scoped; recommend resolving alongside the Administrator-facing configuration screen design, not the data model.
3. **RFID field/table additions (FR-AST-12, FR-SCN-06, R3).** `asset.rfid_tag_id` and the `ScanSourceAdapter` interface reserve the extension point; the actual RFID hardware integration's data needs (read range, tag write history) are deliberately left unspecified until R3 hardware selection, per BRD 1.4.2.
4. **OpenAPI contract.** This document is the data-model/service-signature layer "beneath" the REST API; the actual OpenAPI 3 spec (BRD's "planned" artifact) is a separate downstream deliverable this document feeds, not a substitute for it.
5. **Full visual ERD.** Per the brief's own scope note, a drawn ERD is out of scope for this markdown document — Section 2's field/relationship detail is written to be sufficient for someone to produce one mechanically.
