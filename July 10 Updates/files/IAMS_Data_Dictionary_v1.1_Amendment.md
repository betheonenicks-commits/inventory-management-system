# Database ER Diagram & Data Dictionary — v1.1 Amendment
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-DD-1.1 | **Status:** For Ratification | **Date:** 2026-07-10 | **Applies to:** IAMS-DD-1.0 (read the two together; where they conflict, this amendment governs)

> Closes review findings **N-05** (audit state machine mismatch + missing entities), **N-06** (person vs user account), **N-07** (nullable department), **N-08** (multi-auditor), **N-09** (depreciation parameters), and the FR-ID reconciliation (FRS 2.0 Appendix B). Schema conventions from DD 1.0 §1 (UUID PKs, audit fields, append_only/PII tags, JSONB custom fields, MinIO object keys, optimistic locking) apply to every entity below.

## A. Changes to Existing Entities

### A.1 `audit` — status aligned to the API state machine
`status` enum is now: **`DRAFT` / `IN_PROGRESS` / `PENDING_APPROVAL` / `CLARIFICATION_REQUESTED` / `CLOSED` / `CANCELLED`** (replaces Scheduled/In Progress/Completed). `Scheduled` is represented by `DRAFT` + `scheduled_start`. Add columns: `scheduled_start DATE`, `scheduled_end DATE`, `submitted_at TIMESTAMP nullable`, `closed_at TIMESTAMP nullable`, `routed_approver_user_id UUID FK→user_account nullable` (FR-AUD-22 reroute), `routing_reason VARCHAR(30) nullable`, `version INTEGER NOT NULL DEFAULT 0` (optimistic locking extended per DD 1.0 §7 open decision — **confirmed**). Remove `auditor_user_id` (superseded by `audit_assignment`, §B.3).

### A.2 `audit_finding`
Add `version INTEGER NOT NULL DEFAULT 0` (pre-submission edits only; post-submission immutable). FR reference for immutability remains FR-AUD-18.

### A.3 `user_account` — person split & nullable department
- `department_id` → **nullable** (external reviewers, board viewers, service accounts).
- Remove `name`/`email` duplication of person data for human users: add `person_id UUID FK→person nullable` (null for service accounts). `email` retained as the login identifier (UNIQUE, PII).
- `status` enum extended: `Active / Deactivated / Offboarding / Anonymized`.
- `pending_asset_review` retained; FR reference corrected to **FR-USR-08** (was draft "FR-USR-05").

### A.4 `asset`
- `assigned_to_user_id` → **`assigned_to_person_id UUID FK→person nullable`** (custodians need not be login users; FR-ORG-04).
- Add `serial_number VARCHAR(80) nullable, indexed` (FR-SRC-02), `model_number VARCHAR(80) nullable`, `in_service_date DATE nullable` (depreciation basis), `parent_asset_id UUID FK→asset nullable` (FR-AST-04), `depreciation_method VARCHAR(20) nullable`, `useful_life_months INTEGER nullable`, `salvage_value DECIMAL(12,2) nullable` (per-asset overrides; category defaults in §A.5), `fx_rate_to_reporting DECIMAL(14,6) nullable`, `fx_rate_as_of DATE nullable`, `reporting_currency_amount DECIMAL(12,2) nullable` (FR-INV-10 stored-at-entry rule).
- `insurance_policy` entity: FR reference corrected to **FR-AST-14**.

### A.5 `asset_category`
Add depreciation defaults (FR-AST-16): `default_depreciation_method VARCHAR(20) nullable`, `default_useful_life_months INTEGER nullable`, `default_salvage_pct DECIMAL(5,2) nullable`.

### A.6 `inventory_item`
Add `unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'each'` (FR-INV-11). Batch/expiry moves to `inventory_batch` (§B.9); `expiry_date` on the item is deprecated.

### A.7 `integration`
Credential-storage note now cites **FR-SEC-15 / NFR-SEC-09** (was "FR-SEC-11"). Add `compliance_reviewed_by UUID FK→user_account nullable`, `compliance_reviewed_at TIMESTAMP nullable` (FR-INT-05, BRD §6.5).

### A.8 `notification_pref`
FR reference **FR-NTF-05** now resolvable in FRS 2.0. Unchanged otherwise.

## B. New Entities

### B.1 `person` *(FR-ORG-04; PII cluster root)*
| Field | Type | Constraint | Notes |
|---|---|---|---|
| person_id | UUID | PK | |
| full_name | VARCHAR(120) | NOT NULL — PII | |
| email | VARCHAR(255) | nullable — PII | Contact, not login |
| phone | VARCHAR(30) | nullable — PII | |
| person_type | VARCHAR(15) | NOT NULL | employee / volunteer |
| department_id | UUID | FK→department, nullable | |
| status | VARCHAR(20) | NOT NULL | Active / Departed / Anonymized |
| pseudonym_ref | VARCHAR(30) | UNIQUE, NOT NULL | Stable reference surviving anonymization (FR-CMP-02) |

### B.2 `audit_scan` *(FR-AUD-04/05/19; append_only)*
| Field | Type | Constraint | Notes |
|---|---|---|---|
| scan_id | UUID | PK | |
| audit_id | UUID | FK→audit | |
| idempotency_key | UUID | UNIQUE(audit_id, key), NOT NULL | Replay-safe (offline sync) |
| scan_value | VARCHAR(64) | NOT NULL | Raw scanned payload |
| resolution | VARCHAR(15) | NOT NULL | VERIFIED / DUPLICATE / UNRECOGNIZED / OUT_OF_SCOPE |
| resolved_asset_id | UUID | FK→asset, nullable | Null when UNRECOGNIZED |
| duplicate_of_scan_id | UUID | FK→audit_scan, nullable | FR-SCN-04 |
| scanned_by / scanned_at | UUID / TIMESTAMP | NOT NULL | |
| device_id / method | VARCHAR(60)/VARCHAR(10) | NOT NULL | CAMERA / USB / BT |
| synced_offline | BOOLEAN | NOT NULL DEFAULT false | FR-AUD-19 |

### B.3 `audit_assignment` *(FR-AUD-02, reassignment history; append_only)*
audit_id FK→audit; auditor_user_id FK→user_account; scope_node_id FK→org_node nullable (sub-scope in bulk audits); assigned_at / unassigned_at TIMESTAMP; assigned_by FK→user_account. Active assignment = null `unassigned_at`.

### B.4 `audit_scope_change` *(FR-AUD-23; append_only)*
audit_id FK; asset_id FK; reason VARCHAR(20) (TRANSFERRED / DISPOSED / RETIRED); triggering_event_id FK→lifecycle_event; flagged_at TIMESTAMP; disposition VARCHAR(35) nullable (CONFIRM_VERIFIED_AT_NEW_LOCATION / EXCLUDE_FROM_SCOPE / ACCEPT_AS_EXCEPTION); disposition_by / disposition_at nullable; disposition_note TEXT. Audit closure blocked while any row has null disposition.

### B.5 `asset_reconciliation` *(FR-AUD-21; append_only)*
reconciliation_id PK; asset_id FK; original_finding_id FK→audit_finding; original_audit_id FK→audit; found_at TIMESTAMP; actual_node_id FK→org_node; condition VARCHAR(20); note TEXT; recorded_by / recorded_at. Sets asset status from MISSING; analytics updated per BRD §1.3.1.

### B.6 `job` *(API §8 async pattern)*
job_id PK; job_type VARCHAR(30) (REPORT_EXPORT / BULK_IMPORT_DRYRUN / BULK_IMPORT_COMMIT / LABEL_BATCH / BULK_EXPORT / HR_SYNC); status VARCHAR(12) (PENDING/RUNNING/COMPLETED/FAILED/CANCELLED); progress_percent SMALLINT; submitted_by FK; submitted_at / started_at / completed_at; result_object_key VARCHAR(255) nullable (MinIO); error JSONB nullable. `bulk_import_job` (DD 1.0) becomes a typed extension keyed by job_id.

### B.7 `idempotency_record` *(API §1.7)*
key UUID; actor_user_id FK; endpoint VARCHAR(120); request_hash CHAR(64); response_status SMALLINT; response_body JSONB; created_at. PK (actor_user_id, endpoint, key); purged after 48h (retention_policy-driven).

### B.8 `notification` + `outbox_event` *(FR-NTF-03/08; SRS §2.7)*
`notification`: notification_id PK; user_id FK; event_type VARCHAR(40); title VARCHAR(160); body TEXT; link_path VARCHAR(255) nullable (FR-NTF-10); read_at TIMESTAMP nullable; created_at.
`outbox_event` (append_only): event_id PK; aggregate_type/aggregate_id; event_type; payload JSONB; created_at; dispatched_at nullable; attempts SMALLINT; last_error TEXT nullable; next_retry_at nullable. Drives email/SMS/webhook dispatch with backoff (FR-NTF-08, FR-INT-04).
`delivery_log` (append_only): delivery_id PK; outbox_event_id FK; channel VARCHAR(10) (email/sms/webhook/in_app); recipient VARCHAR(255) — PII; status VARCHAR(12); attempted_at; detail TEXT.

### B.9 `inventory_batch` *(FR-INV-09)*
batch_id PK; item_id FK; batch_code VARCHAR(60); quantity INTEGER NOT NULL; expiry_date DATE nullable; received_at TIMESTAMP.

### B.10 `saved_search` and `report_schedule` *(FR-SRC-04, FR-RPT-13)*
`saved_search`: search_id PK; user_id FK; name VARCHAR(80); resource VARCHAR(30); filter_tree JSONB; created_at. Hard-deletable (no audit-trail obligation).
`report_schedule`: schedule_id PK; report_key VARCHAR(40); params JSONB; cron VARCHAR(40); recipients JSONB; owner_user_id FK; enabled BOOLEAN; last_run_at nullable.

### B.11 `sod_waiver` *(FR-USR-09; append_only + lifted flag)*
waiver_id PK; scope_node_id FK→org_node; action_types JSONB (e.g., ["AUDIT_APPROVAL","TRANSFER_APPROVAL"]); reason TEXT NOT NULL; signed_off_by FK→user_account NOT NULL (must hold IT Security Officer role — app-enforced, non-self-asserted); created_by FK; effective_from / effective_to DATE (null = open); revoked_at / revoked_by nullable.

### B.12 `webhook_subscription` + `webhook_delivery` *(FR-INT-04/06)*
`webhook_subscription`: webhook_id PK; url VARCHAR(255) NOT NULL (Administrator-registered allow-list, NFR-SEC-11); event_types JSONB; signing_secret_ref VARCHAR(255) (secrets-manager ref, FR-SEC-15); enabled BOOLEAN; created_by/at.
`webhook_delivery` (append_only): delivery_id PK; webhook_id FK; event VARCHAR(40); payload_hash CHAR(64); attempt SMALLINT; status_code SMALLINT nullable; delivered_at nullable; error TEXT nullable.

### B.13 `break_glass_session` *(FR-SEC-16; append_only)*
session_id PK; user_id FK; reason TEXT NOT NULL; started_at; expires_at (default start+4h); ended_at nullable; reviewed_by / reviewed_at nullable. Activity during the window is flagged in `security_activity_log` via session_id.

### B.14 `approver_delegation` *(FR-LIF-15)*
delegation_id PK; delegator_user_id FK; delegate_user_id FK; scope_node_id FK nullable; action_types JSONB; effective_from / effective_to DATE; created_by/at. Escalation resolution order: active delegation → department hierarchy → Administrator.

## C. Open Decisions from DD 1.0 §7 — Now Closed
1. JSONB on PostgreSQL: **confirmed** (stack fixed, SRS §7). 2. Polymorphic `legal_hold.entity_id`: **confirmed with application-level integrity checks** + a nightly consistency job. 3. Optimistic locking extended to `audit` and `audit_finding`: **done** (§A.1/§A.2). 4. Global search: **PostgreSQL full-text (tsvector) over existing tables**, no new persisted entity; GIN indexes on asset(name, asset_number, serial_number), person(full_name), vendor(name), purchase_order(po_number).

## D. FR-Reference Corrections Applied Throughout
Per FRS 2.0 Appendix B: insurance → FR-AST-14; vehicle → FR-AST-15; depreciation → FR-AST-16; offboarding → FR-USR-08; SoD waivers → FR-USR-09; webhook signing → FR-INT-06; credential storage → FR-SEC-15. All other DD 1.0 references (FR-CMP-xx, FR-INT-xx, FR-MIG-xx, FR-ANL-xx, FR-AUD-20/21, FR-NTF-05, FR-INV-09/10, FR-SCN-07) are now resolvable in FRS 2.0 under the same numbers.
