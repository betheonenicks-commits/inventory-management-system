# Business Requirements Document (BRD)
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-BRD-2.0 | **Version:** 2.0 (Consolidated Development Baseline) | **Status:** For Ratification | **Date:** 2026-07-10

> **Supersedes:** IAMS_BRD_v1_0.docx, Business_Requirements_Document__BRD_.md, Client_Ready_BRD.md, and all draft references to "IAMS-BRD-3.0/4.0". This is the single authoritative business baseline. Where any earlier document conflicts with this one, this document governs. Section numbering is stable: downstream artifacts (FRS 2.0, SRS 2.0, DD 1.1, API 1.1) cite these section numbers.

## 1. Executive Summary and Objectives

### 1.1 Summary
IAMS is a centralized, auditable, on-premises platform for managing physical assets, quantity-based inventory, organizational structure, asset lifecycle events, and physical audits, for institutions such as schools, colleges, churches, and non-profits. The audit module — mobile scanning, evidence capture, immutable findings, and approval workflow — is the product's core differentiator.

### 1.2 Business Need
Current spreadsheet- and paper-based processes cause duplicate/incomplete records, weak custodianship, slow stock handling, and audits that are lengthy, error-prone, and evidentially weak. A unified digital platform is required to improve visibility, accountability, audit readiness, and decision-making.

### 1.3 Business Objectives and Measures

**BO-1 — Centralize records.** 100% of critical assets registered; ≥95% of active assets and inventory records carry valid location and ownership within 6 months of go-live.
**BO-2 — Reduce loss.** Unexplained missing assets reduced ≥30% from baseline within 12 months.
**BO-3 — Accelerate audits.** Audit preparation effort reduced ≥50%; ≥90% of scheduled audits completed within their window.
**BO-4 — Timely movement recording.** ≥90% of stock movements and asset transfers recorded within 24 hours.
**BO-5 — Data sovereignty.** Zero unreviewed outbound data flows; all analytics remain inside the deployment.

#### 1.3.1 Measurement Integrity Rules
Baselines for BO-2 and BO-3 are captured during migration onboarding (pre-go-live count of untraceable assets and audit effort-hours), owned by the Inventory Manager and Executive Sponsor jointly. A reduction under BO-2 counts **only** when a previously-Missing asset is closed through the formal reconciliation workflow (FR-AUD-21) — never by editing history. Measurement reports are produced from system data (Audit Analytics, FR-AUD-17), not manual tallies.

## 2. Stakeholders and Roles

Stakeholders: Executive Sponsor; Finance/Procurement; Inventory Manager; Auditors (internal and external/read-only); Department Heads; IT/Infrastructure Team; IT Security Officer; Data Protection/Compliance Officer; Employees and Volunteers; Administrators; Vendors.

Default system roles (FRS FR-USR-01): Super Administrator, Administrator, System Operator, Inventory Manager, Auditor, Read-only Auditor, Department Head, Employee/Volunteer, Viewer — plus two **system-provided custom roles** (IT Security Officer, Compliance Officer) built on the custom-role mechanism, and a non-human Integration Service role for external service accounts.

### 2.1 Governance Controls
Separation of duties is a standing control: no user may approve a transfer, disposal, or audit exception they submitted. Organizations too small to separate duties (e.g., a single-administrator parish) may accept the risk through a recorded **SoD Waiver** requiring IT Security Officer sign-off; the sign-off cannot be self-asserted by the requester. Super Administrator emergency ("break-glass") access is permitted only as defined in FRS FR-SEC-16: time-boxed, reason-recorded, dual-notified, and fully logged.

## 3. Problem Statement
Fragmented tracking produces missing or duplicated records, unknown custodianship, delayed movements, low-evidence audits, and no standard workflows for procurement, maintenance, transfer, and disposal. IAMS replaces these with a structured, secure, auditable system.

## 4. Scope

### 4.1 In Scope — 16 Functional Modules
Asset Management (AST); Inventory Management (INV); Organization Management (ORG); Asset Lifecycle (LIF); Audit Management (AUD); Scanning (SCN); Reporting (RPT); Dashboard (DSH); User Management & RBAC (USR); Notifications (NTF); Search (SRC); Security (SEC); Data Migration & Bulk Import/Export (MIG); External Integrations (INT); Compliance & Data Privacy (CMP); Product Analytics (ANL — deployment-local only).

### 4.2 Out of Scope
Live/bidirectional financial accounting or ERP integration (read-only file export of depreciation/valuation data **is** in scope — BR-16); payroll and HR administration (read-only HR/SIS roster sync **is** in scope — BR-16); predictive analytics beyond standard reporting; physical hardware procurement; any hard runtime dependency on external SaaS/cloud services.

## 5. Data and Deployment Principles

### 5.1 Tenancy
IAMS is **single-tenant per deployment**: one installation serves one organization on that organization's own infrastructure. Multi-site scoping within the organization is handled by the organizational hierarchy, never by tenant separation.

### 5.2 On-Premises Operation
The system deploys on the organization's own Linux or Windows Server host via containers, operable by an IT-generalist team, with zero mandatory outbound internet connectivity.

### 5.3 Evidence Integrity
Audit findings, lifecycle events, and transaction history are append-only; corrections are new linked records. Evidence (photos, signatures) is checksummed and access-controlled.

### 5.4 Retention Tiers
Active operational data: retained indefinitely while active. Backups: retained a minimum of **12 months**. Disposed-asset records: archived after **3 years** of inactivity. Security/audit logs: retained a minimum of **7 years** or per the organization's configured policy, whichever is longer. Personal data: retained per configured retention policy, subject to legal hold; data-subject export/erasure supported. All values are deployment-configurable floors via the CMP module.

## 6. Governance of Change and Integrations

6.1 Master-data ownership is assigned to named business users before go-live. 6.2 Approval workflows and naming conventions are configured during onboarding using system defaults defined in the FRS. 6.3 Requirement changes flow through this baseline (BRD → FRS → SRS → DD/API) with version control. 6.4 Document precedence: BRD (business intent) → FRS (functional scope) → SRS (technical envelope) → DD/API (implementation contracts). **6.5 Integration sign-off:** no integration constituting an outbound data flow may be enabled without recorded Compliance Officer review; security-relevant sign-offs cannot be self-asserted by the requester. This control is enforced in software, not only in process.

## 7. Business Requirements (BR Catalog)

| BR | Requirement | Modules |
|----|-------------|---------|
| BR-01 | Maintain a complete, searchable register of individually tracked assets with unique identifiers, labels, custodianship, and full history | AST, SCN |
| BR-02 | Model the organization's physical and organizational structure and scope all records to it | ORG |
| BR-03 | Provide fast identification of any asset or stock item by scan or lookup | SCN, SRC |
| BR-04 | Manage the full asset lifecycle from purchase request through disposal/donation with approvals | LIF |
| BR-05 | Provide end-to-end physical audit capability with mobile scanning, evidence, immutability, approval, and certification — the core differentiator | AUD |
| BR-06 | Track quantity-based inventory with atomic stock movements, reorder alerts, valuation, and vendor history | INV |
| BR-07 | Authenticate every user securely (local, LDAP/AD, or SSO) with configurable policies | SEC |
| BR-08 | Provide role- and scope-filtered dashboards of assets, audits, stock, and expirations | DSH |
| BR-09 | Notify users proactively of audits, expiries, low stock, and pending approvals | NTF |
| BR-10 | Provide standard reports exportable to PDF/Excel/CSV with scheduling | RPT |
| BR-11 | Enforce role-based, org-scoped access on every protected action, at API and UI | USR |
| BR-12 | Migrate existing spreadsheet/legacy data safely with dry-run validation and reconciliation | MIG |
| BR-13 | Preserve a complete, immutable audit trail of all critical actions | SEC, AUD |
| BR-14 | Support configurable custom fields per asset category without code changes | AST |
| BR-15 | Operate fully on-premises with no mandatory external dependencies | (SRS) |
| BR-16 | Support governed, read-only external data exports (accounting, HR/SIS sync, webhooks), disabled by default | INT |
| BR-17 | Enforce configurable data-retention and legal-hold policies | CMP |
| BR-18 | Support data-subject export and erasure/anonymization of personal data | CMP |
| BR-19 | Meet WCAG 2.1 AA accessibility | (SRS) |
| BR-20 | Capture deployment-local usage analytics and user feedback; never transmit outside the deployment | ANL |
| BR-21 | Enforce separation of duties on approvals, with recorded waivers for small organizations; support safe user offboarding | USR |

## 8. Delivery and Release Plan

### 8.1 Release Goals
**R1 (MVP/Core):** "an organization can stop using spreadsheets" — asset register, org structure, users/RBAC/SoD, authentication and security core, migration import, compliance foundation, basic search and lookup. **R2 (Differentiator):** audit module end to end, lifecycle workflows, inventory, notifications, dashboards, full reporting. **R3 (Scale & Integration):** external integrations, bulk export, advanced analytics maturity.

### 8.2 Release Mapping (module level)
R1: AST (excl. depreciation calc), ORG, USR, SEC (excl. optional MFA-for-all, IP restrictions), MIG (import), CMP, SRC (basic), SCN (lookup). R2: AUD, LIF, INV, NTF, DSH, RPT, remaining SEC/SRC items, ANL. R3: INT (except identity-provider auth, which is R1 under SEC), MIG (export). Requirement-level mapping: FRS Section 5 appendix.

## 9. Compliance and Regulatory Requirements
Deployments select a compliance posture per sector/jurisdiction. Named target regimes: **GDPR** (EU-influenced organizations), **India DPDP Act 2023** (Indian deployments), **FERPA-aligned practices** (US educational institutions). The CMP module's retention, legal hold, privacy notices, export/erasure, and data-residency confirmation are the mechanisms; the deploying organization's Compliance Officer selects and owns the posture. IAMS itself never transmits data off-premises (BO-5).

## 10. Risks, Assumptions, Success Criteria
Risks and mitigations, assumptions (training, labels, devices, connectivity within facilities), and acceptance thresholds carry forward from the prior BRDs unchanged in intent; acceptance is now measured through the Given/When/Then acceptance criteria (IAMS-AC-1.0) and the BO measures in §1.3.

## 11. Constraints
11.1 Technology stack is fixed per SRS §3 and changes require stakeholder re-approval. **11.2 Operational simplicity:** every design decision must be operable by a small IT-generalist team — single-command deployment, polling over push transports, no external message broker at initial release, restore-from-backup without vendor involvement. 11.3 Budget/timeline may constrain R2/R3 sequencing but not R1 scope.

## Appendix A — Conflict Resolutions vs Prior BRDs
Availability: **99.5%** during defined operating hours (was 99% in two drafts) — per SRS NFR-AVAIL-01. Concurrency: **100 concurrent users** AND **100,000 assets / 1M history rows** (the two prior figures measured different dimensions; both now stated). Phasing: **R1/R2/R3** as §8 (replaces both the 2-phase and 3-phase drafts). Accounting integration: **read-only export in scope (R3)**; live/bidirectional integration out of scope — resolves the prior in/out-of-scope contradiction. Module count: **16** (adopts MIG/INT/CMP/ANL formally).
