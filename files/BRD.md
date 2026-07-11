**Business Requirements Document**

**Inventory Audit Management System (IAMS)**

*Enterprise On-Premises Asset & Audit Platform*

Document ID: IAMS-BRD-3.0

Document Version: 3.0

Status: Draft for Review (PM Adversarial Review Remediated)

Date: July 2026

# Table of Contents

*(Right-click and select “Update Field” in Word to populate this table after opening the document.)*

# Document Control

## Revision History

| **Version** | **Date**   | **Author**                | **Description**                                                                                                                                                                                                                                                                                                         |
|-------------|------------|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 0.1         | 2026-07-05 | Product Architecture Team | Initial draft derived from master project charter                                                                                                                                                                                                                                                                       |
| 1.0         | 2026-07-05 | Product Architecture Team | First complete BRD for stakeholder review                                                                                                                                                                                                                                                                               |
| 2.0         | 2026-07-06 | Product Architecture Team | Adversarial review remediation: added tenancy decision, compliance/regulatory scope, cost/timeline framing, quantified success criteria, training/support model, expanded risk register, sign-off section                                                                                                               |
| 3.0         | 2026-07-06 | Product Management        | PM adversarial review remediation: added Document ID, completed glossary, fixed terminology consistency, added baseline-metrics requirement, separation-of-duties rule, go-live escalation authority, MVP/release-scope definition, competitive positioning, product feedback loop, usage-analytics requirement (BR-20) |
| 4.0         | 2026-07-09 | Product Management        | Third-pass adversarial review remediation: fixed R1 MVP sequencing contradiction (split BR-08/BR-15 into basic/full scope so R1 is actually usable without spreadsheets), fixed BO-1 baseline circularity, added BO-2 anti-gaming and sampling-interaction notes, added enforceable separation-of-duties requirements (BR-21) and mid-audit scope-integrity requirement (BR-22) |

## Related Documents

- Functional Requirements Specification (FRS) — IAMS-FRS-3.0

- Software Requirements Specification (SRS) — IAMS-SRS-3.0

- User Personas, Roles & Use Cases — IAMS-PUC-1.0

- (Planned) Database ER Diagram & Data Dictionary

- (Planned) REST API Specification (OpenAPI/Swagger)

- (Planned) Security & Compliance Control Matrix

# 1. Introduction

## 1.1 Purpose

This Business Requirements Document (BRD) defines the business case, objectives, scope, and stakeholder needs for the Inventory Audit Management System (IAMS) — an on-premises platform for managing physical assets and conducting comprehensive audits across organizations such as schools, colleges, universities, churches, non-profits, and corporate offices.

This document is intentionally organization-type agnostic: it defines a single configurable data model and workflow set capable of serving any of the target sectors without sector-specific forks. Where a requirement differs by sector, it is expressed as a configuration option rather than a separate feature.

## 1.2 Business Background

Organizations that manage significant physical inventory — computers, furniture, lab equipment, musical instruments, AV equipment, vehicles, tools, consumables — routinely struggle with three problems: asset visibility, audit rigor, and loss/accountability. These problems compound with scale: an organization with a few hundred assets can survive on spreadsheets; one with 50,000–100,000+ assets across multiple buildings and departments cannot.

A further constraint drives the entire architecture: many target organizations operate on constrained IT budgets and are often required, by policy, funding-source rules, or network restriction, to keep sensitive asset, financial, and personal data inside their own network. IAMS is therefore designed for full on-premises deployment with no mandatory external/internet dependency, and with data protection and security treated as first-class requirements rather than an afterthought.

## 1.3 Business Objectives

| **ID** | **Objective**                                                 | **Success Measure (Quantified)**                                                                                                                                                       |
|--------|---------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BO-1   | Establish a single source of truth for all physical assets    | ≥ 98% of assets above the organization's tagging threshold registered with a unique identifier within 90 days of go-live                                                               |
| BO-2   | Reduce asset loss and shrinkage                               | ≥ 25% reduction in assets classified “Missing” at audit close, comparing the second full audit cycle to the first, on a like-for-like scope                                            |
| BO-3   | Make periodic audits fast, evidence-based, and auditable      | ≥ 40% reduction in average audit cycle time (hours per 1,000 assets) versus the organization's documented pre-IAMS process, measured after two audit cycles                            |
| BO-4   | Improve financial and operational reporting                   | Core reports (asset register, depreciation, audit compliance) generated by a non-technical Administrator without developer assistance, in under 5 minutes, on first attempt during UAT |
| BO-5   | Operate entirely within the organization's network            | Zero mandatory outbound internet calls verified during security review prior to go-live                                                                                                |
| BO-6   | Scale from small to very large deployments without redesign   | Verified performance targets (Section 4, SRS) met at both ~500 assets and 100,000+ assets in load testing                                                                              |
| BO-7   | Meet applicable data protection and accessibility obligations | Zero open Critical/High findings in the pre-go-live compliance and security review (Section 6)                                                                                         |

## 1.3.1 Baseline Metrics Capture

The quantified targets in BO-1 through BO-3 are only testable against a known starting point. Before Phase 1 go-live, each deploying organization's current missing-asset rate, average audit cycle time, and reporting turnaround time shall be measured and recorded as the baseline of record. Success against BO-1–BO-3 is evaluated relative to this documented baseline, not against an assumed industry average. Where an organization has no prior audit process to baseline against (e.g., it has never conducted a formal audit), the first IAMS-run audit cycle itself becomes the baseline, and BO-2/BO-3 are evaluated starting from the second cycle.

**BO-1 denominator note (adversarial review v4.0):** Most target organizations do not reliably know their true total asset count before IAMS — that is the problem BO-1 exists to solve, which means "98% of assets registered" cannot be measured against a number IAMS itself produces without becoming circular. To break the circularity, Phase 0 shall include an independent, physical wall-to-wall count (or a statistically valid sample count with extrapolation, for very large estates) conducted and signed off by the Inventory Manager before bulk registration begins. This physical count, not the running count of records created in IAMS, is the denominator of record for BO-1.

**BO-2 measurement-integrity note (adversarial review v4.0):** "Like-for-like scope" in BO-2 is not self-enforcing. To prevent the metric being satisfied by narrowing scope or reclassifying findings rather than genuine improvement: (a) the organizational-hierarchy scope and asset-category scope compared between the first and second audit cycle shall be identical, or any difference explicitly documented and excluded from the comparison; (b) an asset reclassified from Missing to another status between cycles counts toward the reduction only if closed via the reconciliation workflow (FR-AUD-21), not via an unlinked status edit. Where a scope is audited using statistically valid sampling (FR-AUD-20) rather than 100% verification, the Missing-asset rate for that cycle is an estimate with a stated confidence interval, and BO-2 comparison across cycles is only valid if both cycles used the same sampling methodology and confidence level; comparing a sampled cycle to a fully-verified cycle is not a valid like-for-like comparison and shall be flagged as such in Audit Analytics (FR-AUD-17).

## 1.4 Scope

### 1.4.1 In Scope

- Asset registration, categorization, and lifecycle tracking

- Inventory (quantity-based) tracking for consumables and spare parts, including expiry-dated items

- Configurable organizational hierarchy (campus/building/department/etc.), supporting multiple sites within a single organization

- Full asset lifecycle workflows: procurement through disposal/donation, including insurance and depreciation

- Comprehensive audit module: planning, sampling, execution, verification, exception handling, reconciliation, and sign-off

- Barcode/QR code generation and scanning to defined symbology standards, with an architecture that accommodates RFID later

- Role-based access control, including LDAP/Active Directory and optional SSO integration

- Bulk data migration tooling (import/export) with validation and reconciliation reporting

- Defined integration points with external accounting/ERP and HR/student-information systems

- Reporting, dashboards, and notifications

- Data privacy and retention controls, and WCAG 2.1 AA accessibility conformance

- On-premises deployment via Docker Compose on Linux or Windows Server, with defined backup/DR targets

### 1.4.2 Out of Scope (Phase 1)

- Native mobile applications (a responsive/PWA web app is in scope; native iOS/Android apps are a future roadmap item)

- GIS/floor-plan mapping (future roadmap item)

- AI-assisted inventory analytics beyond standard reporting (future roadmap item)

- Physical RFID hardware integration (architecture will support it; hardware integration is future roadmap item)

- Payroll, general ledger, or full financial ERP functionality — IAMS produces asset valuation/depreciation data and can export it, but is not a general ledger system

- True multi-tenant SaaS hosting of multiple unrelated organizations on one shared instance (see Section 5 — each deployment serves one organization, which may itself span multiple sites)

# 2. Stakeholders

| **Stakeholder**                      | **Interest / Role**                                                                                   |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| Super Administrator                  | Owns system configuration, security policy, and organizational hierarchy setup                        |
| Administrator                        | Manages users, day-to-day configuration, and cross-department operations                              |
| Inventory Manager                    | Owns asset registration, procurement linkage, warehouse/stock operations                              |
| Auditor                              | Plans and executes audits, records findings, submits exception reports                                |
| Read-only Auditor                    | Reviews audit data and evidence without edit rights (e.g. external/compliance reviewer)               |
| Department Head                      | Approves audits and transfers for their department; reviews department-level reports                  |
| Employee / Volunteer                 | Is assigned assets; participates in acknowledgment/assignment workflows                               |
| Viewer                               | Read-only access to reports and dashboards (e.g. board member, finance officer)                       |
| IT / Infrastructure Team             | Owns on-premises deployment, backups, upgrades, and integration with LDAP/AD                          |
| Data Protection / Compliance Officer | Owns data retention policy, privacy-notice content, and response to data-subject requests (Section 6) |
| IT Security Officer                  | Owns security review sign-off, vulnerability management, and incident response for the deployment     |
| Finance / Accounting Office          | Consumes depreciation and valuation data; owns any downstream accounting integration                  |

## 2.1 Separation of Duties

Where organizational size permits, the individual(s) responsible for registering an asset and recording its valuation shall not be the same individual who approves that asset's audit findings, and the individual assigning/transferring an asset shall not be the sole approver of its own transfer. Very small organizations (e.g., a single-site non-profit with one Administrator) may formally accept this control as not applicable; that acceptance shall be documented, not silently assumed, and reviewed by the IT Security Officer during the pre-go-live security review (Section 6.5).

**Enforcement note (adversarial review v4.0):** Prior versions of this BRD stated this rule as policy without a corresponding functional requirement, leaving it unenforced in the system. This is now a system-enforced control (BR-21, FR-USR-06, FR-USR-07, FR-AUD-22): self-approval is blocked in software by default, and the waiver described above is itself a recorded, auditable object — not an informal exception.

# 3. Current State and Problem Statement

Most target organizations currently rely on a combination of spreadsheets, paper tags, and informal knowledge to track assets. This produces recurring issues that IAMS is designed to directly address:

| **Current State Pain Point**                                          | **Business Impact**                                                                                                                     | **IAMS Response**                                                                      |
|-----------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| No unique, durable asset identifiers                                  | Assets cannot be reliably matched across records; duplicate or ambiguous entries                                                        | Mandatory unique asset numbers with standardized barcode/QR generation at registration |
| Audits are annual, manual, and paper-based                            | Low audit frequency; findings are not comparable across cycles; no evidence retained                                                    | Structured audit module with sampling, photo evidence, and immutable history           |
| No visibility into who has what                                       | Assets “walk off” with staff/volunteers with no accountability trail                                                                    | Assignment/transfer workflows tied to identity records, including offboarding recovery |
| Maintenance, warranty, and insurance tracked informally or not at all | Preventable failures, missed claims, unplanned costs                                                                                    | Warranty/AMC/insurance tracking with automated expiry notifications                    |
| Reporting requires manual spreadsheet compilation                     | Slow, error-prone board/management reporting                                                                                            | On-demand and scheduled reports, plus direct export to accounting/ERP systems          |
| Data sensitivity and privacy concerns with cloud tools                | Some organizations are unable or unwilling to store asset/financial/personal data off-premises, or are legally restricted from doing so | Fully on-premises deployment; explicit data-residency and privacy controls (Section 6) |
| No accessible interface for staff/volunteers with disabilities        | Excludes qualified users; potential legal exposure for public-facing institutions                                                       | WCAG 2.1 AA conformance target (Section 6.3)                                           |

# 4. Business Requirements Overview

The following table summarizes business-level requirements. Each maps to detailed functional requirements in the FRS. Requirement IDs use the prefix BR.

| **ID** | **Business Requirement**                                                                                                                                                                                                                         | **Priority**         |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------|
| BR-01  | Provide a single system of record for all physical assets across the organization                                                                                                                                                                | Must Have            |
| BR-02  | Support configurable organizational hierarchy appropriate to the deploying organization's structure, including multiple sites within one organization                                                                                            | Must Have            |
| BR-03  | Provide barcode and QR code generation and scanning to a defined standard, with an architecture extensible to RFID                                                                                                                               | Must Have            |
| BR-04  | Provide a full asset lifecycle: procurement, receiving, registration, assignment, transfer, maintenance, retirement, disposal, donation                                                                                                          | Must Have            |
| BR-05  | Provide a comprehensive audit module supporting multiple audit types, statistical sampling at scale, evidence capture, exception handling, reconciliation, and sign-off                                                                          | Must Have            |
| BR-06  | Provide quantity-based inventory tracking, including expiry-dated consumables, across multiple warehouses with inter-warehouse transfer                                                                                                          | Must Have            |
| BR-07  | Provide role-based access control with LDAP/Active Directory and optional SSO integration                                                                                                                                                        | Must Have            |
| BR-08  | Provide dashboards and reports covering assets, audits, maintenance, and compliance, exportable to PDF/Excel/CSV                                                                                                                                 | Must Have            |
| BR-09  | Provide notifications, with per-user preference control, for audits, warranty/AMC/insurance expiry, low stock, approvals, and transfers                                                                                                          | Should Have          |
| BR-10  | Support offline-friendly audit workflows for environments with intermittent network access within the facility                                                                                                                                   | Should Have          |
| BR-11  | Be deployable entirely on-premises via Docker Compose on Linux or Windows Server, without mandatory internet access                                                                                                                              | Must Have            |
| BR-12  | Scale to 100,000+ assets and multiple concurrent users without architectural redesign                                                                                                                                                            | Must Have            |
| BR-13  | Maintain complete, immutable audit trails for asset history and audit activity, with legal-hold support                                                                                                                                          | Must Have            |
| BR-14  | Support future extensibility: RFID, native mobile apps, GIS mapping, AI-assisted analytics                                                                                                                                                       | Could Have (roadmap) |
| BR-15  | Provide bulk data migration tooling (import/export) with validation and reconciliation reporting                                                                                                                                                 | Must Have            |
| BR-16  | Provide defined, secure integration points with external accounting/ERP and HR/student-information systems                                                                                                                                       | Should Have          |
| BR-17  | Meet WCAG 2.1 AA accessibility conformance for the web application                                                                                                                                                                               | Must Have            |
| BR-18  | Meet applicable data protection obligations for personal data of employees, volunteers, and (where linked) students                                                                                                                              | Must Have            |
| BR-19  | Apply defined security controls (authentication, encryption, secrets management, vulnerability management) uniformly across the application, its data stores, and all integrations                                                               | Must Have            |
| BR-20  | Capture local, privacy-respecting usage analytics (which features/modules are actually used) so the product roadmap can be prioritized using evidence rather than assumption, without transmitting usage data outside the organization's network | Should Have          |
| BR-21  | Enforce separation-of-duties controls in software by default (block self-approval of one's own registration/audit/transfer actions), with an auditable waiver mechanism for organizations too small to practically maintain them (Section 2.1)          | Must Have            |
| BR-22  | Preserve audit scope integrity when an asset is transferred, reassigned, or its status changes while an audit covering it is still open, so a legitimate mid-audit move is never silently misreported as Missing                                          | Must Have            |

# 5. Deployment Model and Tenancy

This section resolves an ambiguity identified during review: whether one IAMS deployment can serve multiple independent organizations, and how multi-site organizations are handled.

## 5.1 Tenancy Decision

IAMS Phase 1 uses a single-tenant-per-instance model: each deployment of IAMS serves exactly one legal organization (e.g., one school, one diocese, one company). An organization that itself has multiple physical sites (a school district's schools, a diocese's parishes, a company's branch offices) is represented within one instance using the configurable organizational hierarchy (Campus/Building/... — see FRS Section 2.3), not as separate tenants.

True multi-tenant SaaS hosting — multiple unrelated organizations sharing one running instance with logical data isolation — is explicitly out of scope for Phase 1. This decision is driven by the on-premises, data-sovereignty-first design goal (BO-5, BO-7): an organization that wants to keep its data under its own physical control cannot share infrastructure with another organization's data by definition. This decision should be revisited only if a future hosted/managed-service offering is pursued, at which point it becomes a distinct product line with its own security and compliance review, not a retrofit of the on-premises product.

## 5.2 Multi-Site Consideration

Because a single organization may span multiple physical locations, the data model must support scoping every asset, user, and audit to a specific node in the hierarchy, and must support role scoping so that, for example, a Department Head at one campus cannot see or act on another campus's assets unless explicitly granted. This is carried into FRS FR-ORG-05 and FR-USR-04.

# 6. Regulatory and Compliance Requirements

IAMS handles personal data (employees, volunteers, and in some deployments students), financial data (purchase cost, depreciation), and, for public-facing or publicly funded institutions, may be subject to accessibility law. This section defines the compliance posture; detailed controls are in the FRS (module CMP) and SRS (Security Architecture).

## 6.1 Data Privacy

- IAMS shall support the data-minimization and purpose-limitation principles common to data protection frameworks (e.g., GDPR-style regimes, U.S. state privacy laws): personal data fields shall be limited to what each workflow requires, and shall be configurable per deployment to match the organization's applicable law.

- IAMS shall support a configurable data retention policy per entity type (employee/volunteer records, audit evidence, login logs) and a documented process to anonymize or delete personal data on request or at policy expiry, without breaking the integrity of historical asset/audit records (see FRS FR-CMP-01, FR-CMP-02).

- For deployments in education, IAMS shall be configured so that any linkage between assets and student identities (e.g., a laptop assigned to a student) is treated as protected educational data consistent with the organization's obligations (e.g., FERPA in the United States), with access restricted to authorized roles only.

- Because IAMS is on-premises by design, data residency is inherently satisfied: personal and financial data does not leave the organization's own infrastructure unless the organization explicitly configures an integration (Section 6.4) to do so.

## 6.2 Financial and Audit Standards

- Depreciation calculations shall support methods aligned with common financial reporting standards (e.g., straight-line and declining-balance, consistent with GAAP/IFRS practice), configurable per asset category, so that finance offices can rely on IAMS output for statutory reporting without manual recalculation.

- All audit and financial-transaction records shall be immutable once submitted, with corrections recorded as new, linked entries — supporting internal-control and external-audit requirements common to non-profit and corporate finance offices.

## 6.3 Accessibility

- The web application shall conform to WCAG 2.1 Level AA, given that several target sectors (public schools, non-profits receiving public funding) may carry a legal obligation (e.g., Section 508 in the United States, or equivalent accessibility laws elsewhere) to provide accessible software to staff and volunteers.

## 6.4 Third-Party Integration Compliance

- Any integration that transmits data outside the organization's own network (e.g., an optional SMS gateway, or a cloud-hosted accounting system the organization chooses to connect) shall be explicitly opt-in, documented to the Data Protection stakeholder, and covered by the organization's own data-processing agreement with that third party — IAMS does not silently introduce an external data flow.

## 6.5 Compliance Governance

- A Data Protection / Compliance Officer stakeholder (Section 2) shall review and sign off on the retention policy, privacy notice content, and any new integration before go-live and before any subsequent integration is enabled.

- An IT Security Officer stakeholder shall sign off on the security review (SRS Security Architecture) before go-live.

# 7. Cost, Budget, and ROI Considerations

This BRD does not fix a dollar budget, since costs vary significantly by organization size and hosting choice (existing on-premises hardware vs. new procurement). It defines the categories that must be estimated and approved before development proceeds, and the qualitative return the investment is expected to produce.

## 7.1 Cost Categories to Be Estimated

| **Category**                  | **Description**                                                                                                                                                                       |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Build / Development           | Engineering effort across backend, frontend, and DevOps for all modules defined in the FRS                                                                                            |
| Infrastructure                | Server hardware or VM allocation, storage for attachments/backups, and network changes needed for on-premises hosting                                                                 |
| Licensing                     | Any commercial dependencies (e.g., commercial LDAP/AD tooling, optional SMS gateway) — the core stack (Java, Spring, React, PostgreSQL, Docker) is open-source with no licensing cost |
| Data Migration                | Effort to clean, map, and import legacy spreadsheet/paper records (see Section 1.4, BR-15)                                                                                            |
| Training & Change Management  | Staff and volunteer training, documentation, and rollout support (Section 9)                                                                                                          |
| Ongoing Support & Maintenance | Post-launch patching, backup verification, and helpdesk support (Section 10)                                                                                                          |

## 7.2 Build vs. Buy Rationale

Commercial asset-management products exist in the market. IAMS is justified as a custom build specifically because of three requirements that are difficult to satisfy with off-the-shelf SaaS tools at the target price point: (1) mandatory on-premises deployment with no internet dependency, (2) an audit module deep enough to serve as the primary differentiator rather than a bolt-on feature, and (3) a single configurable data model spanning education, faith-based, non-profit, and corporate use without per-sector licensing. Organizations for whom none of these three constraints apply should evaluate commercial alternatives before committing to a custom build; this evaluation is a stakeholder decision to be documented outside this BRD, not re-litigated here.

## 7.2.1 Competitive Positioning

| **Alternative**                                                                                        | **Where It's Strong**                                 | **Where IAMS Differentiates**                                                                                                                      |
|--------------------------------------------------------------------------------------------------------|-------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| General-purpose commercial asset trackers (e.g., spreadsheet-plus tools, generic fixed-asset software) | Fast to deploy, low upfront cost, familiar UI         | Audit workflow is typically a shallow add-on; IAMS treats audit sampling, reconciliation, and evidence capture as core, not bolted-on              |
| Cloud-hosted SaaS asset-management platforms                                                           | No infrastructure to manage, frequent feature updates | Cannot satisfy a hard on-premises/data-sovereignty requirement; ongoing per-seat licensing cost compounds at 100,000+ asset scale                  |
| Spreadsheets / manual process (the real incumbent for most target organizations)                       | Zero cost, zero learning curve, total flexibility     | No unique identifiers, no audit trail, no accountability chain — this is the actual competitor IAMS must beat on ease-of-use, not just on features |

The most realistic competitive risk is not a rival product — it is an organization simply continuing to use spreadsheets because IAMS feels like more overhead than it removes. This is why R1 (Section 8.1) is scoped to be usable almost immediately, rather than requiring the full audit module before an organization sees any value.

## 7.3 Expected Return

- Reduction in asset loss (BO-2) translates directly into avoided replacement cost, which should be estimated per organization using its own historical shrinkage data.

- Reduction in audit cycle time (BO-3) translates into staff/volunteer hours recovered for other work.

- Improved warranty/AMC/insurance tracking reduces avoidable maintenance and replacement spend.

- A quantified ROI figure requires organization-specific baseline data and shall be computed during the discovery phase with each deploying organization, not assumed generically here.

# 8. Product Release Scope, MVP, and Timeline

## 8.1 MVP Definition and Prioritization Discipline

Nineteen of twenty business requirements in Section 4 are marked “Must Have.” Read literally, that means nothing can be cut — which defeats the purpose of MoSCoW prioritization and is a review finding in its own right. “Must Have” in Section 4 describes what is required for the complete product vision, not what must exist on day one. This section resolves that by defining an actual Minimum Viable Product and mapping requirements to releases, so “Must Have” is never used as a substitute for sequencing.

The MVP (Release R1) is the smallest release that lets one organization register assets, scope them to a location, control access by role, produce a basic asset register, and load its existing spreadsheet data — enough to actually replace a spreadsheet and start generating trustworthy data, but deliberately without the full audit workflow, which ships in R2. Shipping R1 first, rather than waiting for the full audit module, lets a real organization start using the system and generating the usage-analytics evidence (BR-20) that should inform how R2 and R3 are actually built.

**Sequencing correction (adversarial review v4.0):** An earlier version of this table assigned all of BR-08 (Reporting) and all of BR-15 (Bulk Migration) to later releases while simultaneously claiming R1 "produces a basic asset register" and "lets an organization stop using spreadsheets" — both impossible without a minimal slice of reporting and migration present in R1. BR-08 and BR-15 are therefore split below into a **basic** scope (R1) and a **full** scope (later release), rather than assigned to a single release wholesale.

## 8.2 Release-to-Requirement Mapping

| **Release**                                    | **Business Requirements Delivered**                                          | **Product Rationale**                                                                                                                                                                                                                                                              |
|------------------------------------------------|------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R1 — MVP / Core                                | BR-01, BR-02, BR-03 (basic), BR-07, BR-08 (basic: Asset Register, Employee Asset List, PDF/Excel/CSV export only — FR-RPT-01, FR-RPT-03, FR-RPT-12), BR-11, BR-12, BR-13, BR-15 (basic: CSV/Excel bulk import with dry-run validation and reconciliation report — FR-MIG-01–04), BR-17, BR-18, BR-19, BR-21 | Prove the core data model and security posture with real data before investing further; this is now the smallest release an organization can actually go live on and stop using spreadsheets — it can both load its existing data in and read it back out |
| R2 — Audit & Lifecycle (the differentiator)    | BR-04, BR-05, BR-06, BR-08 (full: remaining report types, dashboards, scheduled reports — all other FR-RPT-\*, FR-DSH-\*), BR-09, BR-20, BR-22 | The audit module is the stated core differentiator (BRD Section 1.2) — it ships as soon as R1 proves the data foundation is solid, not bundled into R1 where a defect in audit logic could block the simpler win of basic asset tracking. BR-22 (audit scope integrity) belongs here because it only has meaning once the audit module exists |
| R3 — Migration, Integration & Scale Validation | BR-10, BR-14, BR-15 (full: bulk export/data-portability, staged large-scale migration tooling), BR-16 | Bulk export, large-scale migration refinements, and external integrations are highest-value once an organization has already validated the product on R1/R2; performance validation at 100,000+ assets belongs here, once real usage patterns from R1/R2 exist to test against |

This mapping supersedes a literal reading of the MoSCoW column in Section 4 for sequencing purposes: a “Must Have” requirement assigned to R3 is still mandatory for the complete product, but is not blocking for go-live on R1. Where a BR is split across releases above, FR-level detail is authoritative — see the FR-level Release Mapping appendix in FRS Section 5, since a single BR can span FRs of very different weight and this BR-level table alone is too coarse for sprint planning.

## 8.3 Deployment Timeline (Per Organization)

Section 8.2 defines what the product delivers in each release. This subsection defines how a single deploying organization rolls those releases out internally, which is a separate concern (configuration and change management pace vs. software release scope).

IAMS shall be delivered iteratively (see Development Approach in the project charter). At the business level, rollout follows four phases per deploying organization:

| **Phase**                                       | **Content**                                                                                                          | **Exit Criteria**                                                                                              |
|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| Phase 0 — Discovery & Configuration             | Confirm organizational hierarchy, roles, custom fields, retention policy, and integration needs for this deployment  | Configuration signed off by Administrator and Compliance Officer                                               |
| Phase 1 — Core Deployment (Release R1)          | Asset Management, Organization Management, User Management/RBAC, core Reporting, on-premises infrastructure stood up | Security review passed; core modules pass UAT                                                                  |
| Phase 2 — Audit & Lifecycle Rollout             | Audit Management, Asset Lifecycle, Inventory, Notifications, first full audit cycle executed                         | First audit cycle completed with signed completion certificate                                                 |
| Phase 3 — Migration, Integration & Optimization | Legacy data migration, any accounting/HR integrations, dashboard tuning, performance validation at target scale      | Data migration reconciliation report shows zero unresolved discrepancies; performance targets met in load test |

Specific calendar dates are not fixed in this BRD, since they depend on the deploying organization's own readiness (data cleanliness, IT resourcing); each phase's duration shall be estimated jointly with the organization during Phase 0.

# 9. Training and Change Management

Adoption risk (Risk R-1) is directly addressed by a defined training requirement, given that target users include non-technical staff and volunteers.

- Role-based training materials (Administrator, Auditor, Department Head, Viewer) shall be produced alongside the User Manual, each scoped to what that role actually does in the system.

- A hands-on training session covering the scanning workflow shall be delivered before the first live audit, since this is the workflow most exposed to adoption failure.

- A designated internal “super user” per department/ministry shall be identified during Phase 0 to provide first-line peer support and reduce load on IT.

- Change-management communication (why the system is being introduced, what changes for each role) shall be delivered before Phase 1 go-live, not after.

# 10. Support and Maintenance Model

Because several target organizations lack dedicated IT staff, the support model must be explicit rather than assumed.

| **Aspect**                 | **Requirement**                                                                                                                                                                                                           |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Patching Cadence           | Security patches applied within 30 days of release for High/Critical severity; documented in the Administrator Guide                                                                                                      |
| Backup Verification        | Automated backups (SRS NFR-AVAIL-02) verified by a successful restore test at least quarterly                                                                                                                             |
| Helpdesk / Support Channel | Each deploying organization shall designate a first-line support contact (internal IT or a super user) before go-live; escalation path to the development/maintenance team shall be documented in the Administrator Guide |
| Incident Response          | A documented incident response procedure (SRS Security Architecture) covers suspected data breaches or security incidents, including notification timelines consistent with applicable law                                |

# 11. Assumptions, Constraints, and Dependencies

## 11.1 Assumptions

- Deploying organizations have at least a basic internal network (LAN/Wi-Fi) covering the facilities where assets are located.

- Organizations performing audits have access to smartphones, tablets, laptops, or dedicated barcode scanners for scanning during audits.

- An organization's existing LDAP/Active Directory (if any) can be reached from the on-premises server hosting IAMS.

- Initial asset data (if migrating from spreadsheets) will be provided in a structured, importable format, or can be reasonably cleaned into one using the bulk import tooling (BR-15).

- The organization has designated a Data Protection / Compliance Officer role (even if part-time / combined with another role) able to approve the retention policy and any integration.

## 11.2 Constraints

- The system must run fully on-premises; no feature may have a hard dependency on an internet connection.

- The technology stack is fixed: Java 21 / Spring Boot 3 backend, React/Material UI frontend, PostgreSQL database, Docker Compose deployment.

- The system must support both Linux and Windows Server hosts.

- Budget and IT staffing at many target organizations is limited, so operational complexity (backup, upgrades, security patching) must be kept low.

- The system must conform to WCAG 2.1 AA and must not introduce a data flow outside the organization's network without explicit, documented opt-in.

## 11.3 Dependencies

- Availability of organizational structure data (departments, buildings, rooms) before go-live configuration.

- Availability of barcode/QR scanning hardware or camera-equipped devices for audit execution.

- For LDAP/AD or SSO integration: availability of a service account and connection details from the organization's IT team.

- For any accounting/ERP or HR/SIS integration: availability of that system's API or export format, and a signed-off data flow from the Compliance Officer.

# 12. Business Risks

| **ID** | **Risk**                                                                                 | **Impact**                                                        | **Mitigation**                                                                                                                       |
|--------|------------------------------------------------------------------------------------------|-------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| R-1    | Low user adoption of scanning workflows during audits                                    | Audits revert to manual/paper, undermining core value proposition | Multiple scanning methods, offline scan queueing, and mandatory pre-go-live training (Section 9)                                     |
| R-2    | Data migration from legacy spreadsheets is incomplete or inconsistent                    | Inaccurate asset register at go-live                              | Bulk import tooling with validation and a reconciliation report (FRS FR-MIG-01–03) plus a Phase 3 reconciliation audit               |
| R-3    | Organization lacks IT staff to operate on-premises infrastructure                        | Deployment stalls or system is not maintained (patches, backups)  | Docker Compose packaging, clear Installation/Administrator Guides, and a defined support model (Section 10)                          |
| R-4    | Very large deployments (100,000+ assets) reveal performance bottlenecks                  | Slow audits, poor user experience at scale                        | Performance testing at target scale is a defined non-functional requirement; audit sampling (FR-AUD-20) reduces per-cycle load       |
| R-5    | Sector diversity leads to feature bloat or a confusing generic UI                        | Poor usability for any single sector                              | Configurable hierarchy/terminology and dynamic custom fields rather than sector-specific hardcoded features                          |
| R-6    | Personal data (employee/volunteer/student-linked) is mishandled or retained indefinitely | Legal/regulatory exposure, loss of trust                          | Configurable retention policy, anonymization workflow, and Compliance Officer sign-off before go-live (Section 6.1)                  |
| R-7    | A future integration silently creates an external data flow                              | Undermines the on-premises/data-sovereignty value proposition     | All integrations are explicitly opt-in, documented, and authenticated (Section 6.4, FRS module INT)                                  |
| R-8    | Security vulnerability in the application, a dependency, or an integration is exploited  | Data breach, loss of trust, potential legal liability             | Defined Security Architecture (SRS), dependency scanning, periodic penetration testing, and incident response procedure (Section 10) |
| R-9    | Application is not usable by staff/volunteers with disabilities                          | Legal exposure for public institutions; excludes qualified users  | WCAG 2.1 AA conformance target verified in UAT (Section 6.3)                                                                         |

# 13. Success Criteria / Definition of Done (Business Level)

- ≥ 98% of assets above the organization's capitalization/tagging threshold are registered in IAMS with a unique barcode/QR code within 90 days of go-live.

- At least one full audit cycle has been completed end-to-end using the system, producing a completion certificate and exception report, with zero unresolved data-integrity issues.

- Role-based access is configured and, where applicable, integrated with the organization's LDAP/Active Directory or SSO provider.

- Management can produce, without developer assistance, the core reports: asset register, missing/damaged assets, audit compliance, warranty/AMC/insurance expiry, depreciation.

- The system is deployed and operating entirely within the organization's internal network, with zero undocumented outbound data flows.

- The system demonstrates acceptable performance at the organization's actual or projected asset count, up to the 100,000+ asset design target, verified by load testing.

- The pre-go-live security and compliance review (Section 6, SRS Security Architecture) is passed with zero open Critical/High findings.

- The application passes a WCAG 2.1 AA accessibility audit.

- The data migration reconciliation report (if migrating legacy data) shows zero unresolved discrepancies.

# 14. Stakeholder Sign-Off

## 14.1 Escalation Authority

Go-live shall not proceed while the IT Security Officer or the Data Protection / Compliance Officer has an open, unresolved objection recorded against the security review (SRS Security Architecture) or the compliance posture (Section 6). If either stakeholder withholds sign-off, the objection escalates to the Executive Sponsor, who shall issue a documented risk-acceptance decision (accept, remediate-then-proceed, or delay) before go-live — sign-off withholding is never silently overridden by schedule pressure.

## 14.2 Sign-Off Record

This BRD requires review and approval from the following roles before functional and technical design proceeds. The tenancy model decision (Section 5.1) is called out separately below because, unlike the other content in this document, it was set as a working default during drafting and requires explicit Executive Sponsor validation against the actual deploying organization's structure before the ER diagram is built on top of it.

| **Role**                                                  | **Name** | **Signature** | **Date** |
|-----------------------------------------------------------|----------|---------------|----------|
| Executive Sponsor                                         |          |               |          |
| Executive Sponsor — Tenancy Model Validated (Section 5.1) |          |               |          |
| Product Owner                                             |          |               |          |
| IT Security Officer                                       |          |               |          |
| Data Protection / Compliance Officer                      |          |               |          |
| Finance / Accounting Representative                       |          |               |          |

# 15. Product Feedback and Continuous Discovery

A BRD of this scope risks becoming a one-time specification that is never revisited once development starts. To keep the product responsive to real usage rather than only to the assumptions made during this drafting pass, the following feedback loop is required post-R1 go-live:

- Usage analytics (BR-20) shall be reviewed monthly by the Product Owner to identify features that are configured but unused, which is a leading indicator that either the feature is miscommunicated (needs training) or was not actually needed (candidate for descoping in a future release).

- A lightweight in-app feedback mechanism (even a simple “report an issue / suggest an improvement” form routed to the Product Owner) shall be available to all roles from R1 onward.

- Support tickets and training pain points (Section 10) shall be triaged quarterly against the Section 8.2 release backlog, so recurring support burden becomes a direct input into re-prioritization rather than being absorbed silently by the helpdesk indefinitely.

- The roadmap items explicitly deferred in Section 1.4.2 (native mobile, GIS mapping, AI-assisted analytics, RFID) shall be re-evaluated at least annually against actual usage-analytics evidence, rather than being treated as permanently out of scope by default.

# 16. Glossary

| **Term**                 | **Definition**                                                                                                                                                                                                         |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Asset                    | A discrete physical item tracked by the system, identified by a unique asset number and (usually) a barcode/QR code.                                                                                                   |
| AMC                      | Annual Maintenance Contract — a recurring service agreement for an asset, tracked for expiry/renewal.                                                                                                                  |
| Audit                    | A structured, time-boxed exercise to verify the existence, location, and condition of a defined set of expected assets.                                                                                                |
| Exception                | An audit finding where an asset's actual state (missing, damaged, wrong location) differs from its expected state.                                                                                                     |
| Organizational Hierarchy | The configurable tree of locations/units used to scope assets and audits, supporting multiple sites within one organization.                                                                                           |
| On-premises              | Deployed and run entirely within the organization's own network infrastructure, without dependency on external/internet services.                                                                                      |
| RBAC                     | Role-Based Access Control — permissions granted based on a user's assigned role rather than individually.                                                                                                              |
| RTO / RPO                | Recovery Time Objective / Recovery Point Objective — the maximum acceptable downtime and data loss after an incident, defined in the SRS.                                                                              |
| PII                      | Personally Identifiable Information — data that can identify an individual (e.g., an employee or volunteer record).                                                                                                    |
| WCAG                     | Web Content Accessibility Guidelines — the standard used to define accessibility conformance (Level AA target).                                                                                                        |
| Data Residency           | The requirement that data physically remain within a defined geographic or network boundary — satisfied here by the on-premises architecture.                                                                          |
| Single-Tenant Deployment | A deployment model where one running instance of the system serves exactly one organization (Section 5.1).                                                                                                             |
| LDAP                     | Lightweight Directory Access Protocol — a standard protocol for accessing an organization's directory of users and groups (e.g., Active Directory).                                                                    |
| SSO                      | Single Sign-On — lets a user authenticate once with an identity provider and access IAMS without a separate login (Section 6.4).                                                                                       |
| GAAP                     | Generally Accepted Accounting Principles — the U.S. accounting standard referenced for depreciation methods (Section 6.2).                                                                                             |
| IFRS                     | International Financial Reporting Standards — the international accounting standard referenced for depreciation methods (Section 6.2).                                                                                 |
| FERPA                    | Family Educational Rights and Privacy Act — U.S. law protecting the privacy of student education records, relevant where assets are linked to student identities (Section 6.1).                                        |
| GDPR                     | General Data Protection Regulation — the EU data protection law used here as a reference model for data-minimization and retention principles (Section 6.1), whether or not it applies directly to a given deployment. |
| MVP                      | Minimum Viable Product — the smallest release (Release R1, Section 8.1) that delivers usable, testable business value.                                                                                                 |
