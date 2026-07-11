**Functional Requirements Specification**

**Inventory Audit Management System (IAMS)**

*Module-by-Module Functional Requirements*

Document ID: IAMS-FRS-4.0 \| Version: 4.0 \| Status: Draft for Review (PM Adversarial Review Remediated)

# 1. Introduction

This Functional Requirements Specification (FRS) decomposes the business requirements defined in the BRD (IAMS-BRD-4.0) into concrete, testable functional requirements, organized by system module. Each requirement is assigned a unique ID of the form FR-\<MODULE\>-\<NUMBER\> to support direct traceability into epics, user stories, and test cases.

## 1.2 Priority vs. Release Sequencing

Priority (MoSCoW) in this document describes what is required for the complete product vision, not release sequencing. Release sequencing (which FRs ship in Release R1 MVP vs. R2 vs. R3) is defined in BRD Section 8.2 at the BR level and, at the FR level, in the Release Mapping appendix (Section 5 of this document) — consult both before treating a “Must Have” requirement as blocking for an early release.

Version 2.0 incorporated remediation of gaps identified during adversarial review: bulk data migration, audit sampling and reconciliation, insurance and vehicle-asset attributes, inter-warehouse transfer, expiry-dated inventory, offboarding, external integrations, and a dedicated Compliance & Data Privacy module. Security requirements were also expanded to explicitly cover integration-layer controls.

Version 4.0 incorporates a third-pass adversarial review: added enforceable separation-of-duties controls (FR-USR-06, FR-USR-07, FR-AUD-22), mid-audit scope-integrity handling (FR-AUD-23), resolved the insurance/vehicle field storage ambiguity in favor of dedicated schema columns (FR-AST-13, FR-AST-14 amended), added a multi-currency reporting-rollup rule (FR-INV-10), split the Data Migration module's R1 vs. R3 scope explicitly (Section 2.13), and added the FR-level Release Mapping appendix (Section 5) so BR-to-FR sequencing no longer relies on BR-level granularity alone.

Priority uses the MoSCoW scale: Must Have, Should Have, Could Have. Unless stated otherwise, all requirements apply uniformly regardless of the deploying organization's sector; sector-specific variation is handled through configuration, not separate requirements.

## 1.1 Requirement ID Convention

| **Module Code** | **Module**                          |
|-----------------|-------------------------------------|
| AST             | Asset Management                    |
| INV             | Inventory Management                |
| ORG             | Organization Management             |
| LIF             | Asset Lifecycle Management          |
| AUD             | Audit Management                    |
| SCN             | Barcode/QR/RFID Scanning            |
| RPT             | Reporting                           |
| DSH             | Dashboard                           |
| USR             | User Management & RBAC              |
| NTF             | Notifications                       |
| SRC             | Search                              |
| SEC             | Security                            |
| MIG             | Data Migration & Bulk Import/Export |
| INT             | External Integrations               |
| CMP             | Compliance & Data Privacy           |
| ANL             | Product Analytics                   |

# 2. Functional Requirements by Module

## 2.1 Asset Management (AST)

Covers registration and ongoing management of individually tracked physical assets, including specialized attributes for insurance and vehicles identified during review.

| **ID**    | **Requirement**                                                                                                                                                                                                                                                       | **Priority** |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-AST-01 | The system shall allow registration of a new asset with a system-generated unique asset number.                                                                                                                                                                       | Must Have    |
| FR-AST-02 | The system shall generate a barcode and/or QR code for each registered asset, renderable and printable as a label.                                                                                                                                                    | Must Have    |
| FR-AST-03 | The system shall support assignment of assets to one or more configurable categories and groups.                                                                                                                                                                      | Must Have    |
| FR-AST-04 | The system shall support parent-child relationships between assets (e.g., a laptop and its charger, or a computer lab bundle).                                                                                                                                        | Should Have  |
| FR-AST-05 | The system shall allow upload of one or more images and file attachments (invoices, manuals, warranty cards) per asset, stored per the file storage architecture defined in the SRS.                                                                                  | Must Have    |
| FR-AST-06 | The system shall support organization-defined custom fields per asset category without requiring code changes.                                                                                                                                                        | Must Have    |
| FR-AST-07 | The system shall track asset status (e.g., In Use, In Storage, Under Repair, Missing, Retired, Disposed) with a configurable status list.                                                                                                                             | Must Have    |
| FR-AST-08 | The system shall track warranty start/end dates and AMC (Annual Maintenance Contract) periods per asset.                                                                                                                                                              | Must Have    |
| FR-AST-09 | The system shall record manufacturer, vendor, purchase date, purchase cost, currency, and purchase order reference per asset.                                                                                                                                         | Must Have    |
| FR-AST-10 | The system shall maintain a complete, append-only history of all changes to an asset (status, location, assignment, condition).                                                                                                                                       | Must Have    |
| FR-AST-11 | The system shall maintain a movement history log recording every change of physical location for an asset.                                                                                                                                                            | Must Have    |
| FR-AST-12 | The system shall provide an architecture-level extension point for RFID tag identifiers, without requiring RFID hardware for Phase 1.                                                                                                                                 | Should Have  |
| FR-AST-13 | The system shall track insurance policy details per asset (insurer, policy number, coverage amount, expiry) where applicable, and support linking a claim record to an audit finding of damage or loss. Insurer, policy number, coverage amount, and expiry are dedicated, first-class, indexed columns (not the JSONB custom-field mechanism of FR-AST-06), because expiry must be efficiently queryable for FR-NTF-01 alerts and FR-RPT-05 reporting at 100,000-asset scale. | Should Have  |
| FR-AST-14 | The system shall support a Vehicle asset subtype with dedicated, first-class, indexed fields for VIN, registration number, mileage/odometer reading, and registration/insurance expiry — for the same query-performance reason as FR-AST-13, not the generic JSONB custom-field mechanism of FR-AST-06. FR-AST-06 custom fields remain reserved for organization-specific, ad hoc fields that do not require cross-organization schema consistency or first-class query performance (see SRS Section 4.1 for the general rule distinguishing the two). | Should Have  |
| FR-AST-15 | The system shall calculate depreciation per asset using an organization-configured method (e.g., straight-line, declining-balance) with configurable useful life and salvage value per category, and expose the calculated value to the Reporting module (FR-RPT-09). | Should Have  |

## 2.2 Inventory Management (INV)

Covers quantity-based tracking of consumables, spare parts, and stock, including expiry and inter-warehouse movement identified during review.

| **ID**    | **Requirement**                                                                                                                                                                                      | **Priority** |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-INV-01 | The system shall track quantity-on-hand for consumable and spare-part inventory items, separate from individually tracked assets.                                                                    | Must Have    |
| FR-INV-02 | The system shall support Stock In and Stock Out transactions with reason codes and responsible user.                                                                                                 | Must Have    |
| FR-INV-03 | The system shall support multiple warehouses/storage locations, each with shelf/bin-level sub-locations.                                                                                             | Must Have    |
| FR-INV-04 | The system shall support configurable reorder levels per inventory item and flag items below threshold.                                                                                              | Must Have    |
| FR-INV-05 | The system shall support manual inventory adjustments with mandatory reason and approver.                                                                                                            | Must Have    |
| FR-INV-06 | The system shall compute inventory valuation using a configurable costing method (e.g., weighted average) and support multi-currency valuation where an organization purchases from foreign vendors. | Should Have  |
| FR-INV-07 | The system shall maintain vendor records and link purchase history to inventory items.                                                                                                               | Must Have    |
| FR-INV-08 | The system shall support Stock Transfer transactions between warehouses, distinct from Stock In/Out, preserving a linked record of source and destination.                                           | Must Have    |
| FR-INV-09 | The system shall support an optional expiry date per inventory batch/lot (e.g., lab chemicals, perishable supplies) and generate expiry-approaching alerts (see FR-NTF-01).                          | Should Have  |
| FR-INV-10 | Every deployment shall configure exactly one Reporting Currency. Where an asset or inventory item's purchase cost (FR-AST-09, FR-INV-06) is recorded in a different transaction currency, the system shall capture the FX rate and its as-of date at the time of transaction entry, store both the original-currency and reporting-currency amounts, and use the stored reporting-currency amount (not a rate looked up at report-generation time) in all aggregate valuation, depreciation, and asset-register totals (FR-RPT-01, FR-RPT-09), displaying the original currency and rate alongside on a per-line basis for audit traceability. | Should Have |

## 2.3 Organization Management (ORG)

Covers the configurable organizational hierarchy used to scope assets, audits, and users, including multi-site support within one organization (BRD Section 5).

| **ID**    | **Requirement**                                                                                                                                                                      | **Priority** |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-ORG-01 | The system shall support a configurable, multi-level organizational hierarchy (e.g., Campus \> Building \> Floor \> Room, or Ministry \> Department).                                | Must Have    |
| FR-ORG-02 | The system shall allow administrators to relabel hierarchy level names to match the organization's terminology.                                                                      | Must Have    |
| FR-ORG-03 | The system shall support Department / Cost Center entities independent of physical location.                                                                                         | Must Have    |
| FR-ORG-04 | The system shall maintain Employee and Volunteer records, including role and department/ministry affiliation, treated as personal data under FR-CMP-01–02.                           | Must Have    |
| FR-ORG-05 | The system shall allow any asset, audit, or user to be scoped to one or more nodes in the organizational hierarchy, supporting multiple sites within a single organization instance. | Must Have    |
| FR-ORG-06 | The system shall support specialized location types (Classroom, Laboratory) as configurable variants of the generic “Room” entity.                                                   | Should Have  |

## 2.4 Asset Lifecycle Management (LIF)

Covers the end-to-end lifecycle of an asset from procurement through disposal.

| **ID**    | **Requirement**                                                                                                                                                                                             | **Priority** |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-LIF-01 | The system shall support creation and approval of Purchase Requests prior to Purchase Order issuance.                                                                                                       | Should Have  |
| FR-LIF-02 | The system shall support Purchase Order creation, tracking, and linkage to received assets.                                                                                                                 | Must Have    |
| FR-LIF-03 | The system shall support a Receiving step that reconciles delivered items against a Purchase Order before asset registration.                                                                               | Should Have  |
| FR-LIF-04 | The system shall support Assignment of an asset to an employee, volunteer, department, or room.                                                                                                             | Must Have    |
| FR-LIF-05 | The system shall support Transfer of an asset between locations, departments, or holders, with approval where configured.                                                                                   | Must Have    |
| FR-LIF-06 | The system shall support logging of Repair events, including vendor, cost, and downtime.                                                                                                                    | Must Have    |
| FR-LIF-07 | The system shall support scheduling and logging of Preventive Maintenance on a recurring basis.                                                                                                             | Should Have  |
| FR-LIF-08 | The system shall support logging of Corrective Maintenance events triggered by faults or audit findings.                                                                                                    | Must Have    |
| FR-LIF-09 | The system shall support Retirement, Disposal, and Donation workflows, each capturing reason, approver, and disposition date, including environmentally compliant e-waste disposal method where applicable. | Must Have    |
| FR-LIF-10 | The system shall retain a complete, immutable transaction history for every lifecycle event on an asset.                                                                                                    | Must Have    |

## 2.5 Audit Management (AUD) — Core Differentiator

The audit module is the primary differentiator of IAMS. Sampling, reconciliation, and condition-scale requirements below close gaps identified for operation at scale.

| **ID**    | **Requirement**                                                                                                                                                                                                                                                                 | **Priority** |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-AUD-01 | The system shall support creation of an audit scoped by type: Quarterly, Annual, Department, Room, Building, Campus, or Surprise.                                                                                                                                               | Must Have    |
| FR-AUD-02 | The system shall support Bulk Audits covering multiple scopes in a single coordinated audit exercise.                                                                                                                                                                           | Should Have  |
| FR-AUD-03 | The system shall generate an expected-asset list for an audit based on its defined scope at the time of audit creation.                                                                                                                                                         | Must Have    |
| FR-AUD-04 | The system shall support asset verification via barcode/QR scan, recording verifier, timestamp, and device.                                                                                                                                                                     | Must Have    |
| FR-AUD-05 | The system shall support Continuous Scan Mode allowing an auditor to scan a sequence of assets without repeated manual confirmation steps.                                                                                                                                      | Must Have    |
| FR-AUD-06 | The system shall support Batch Scanning for auditing multiple co-located assets in a single operation.                                                                                                                                                                          | Should Have  |
| FR-AUD-07 | The system shall compute and display, in real time, Expected vs. Verified asset counts for an in-progress audit.                                                                                                                                                                | Must Have    |
| FR-AUD-08 | The system shall classify unverified expected assets as Missing at audit closure.                                                                                                                                                                                               | Must Have    |
| FR-AUD-09 | The system shall allow an auditor to flag a verified asset as Damaged and record a structured condition assessment against a configurable condition scale (e.g., Good / Fair / Minor Damage / Major Damage / Unusable).                                                         | Must Have    |
| FR-AUD-10 | The system shall allow attachment of photo evidence to any audit finding.                                                                                                                                                                                                       | Must Have    |
| FR-AUD-11 | The system shall allow the auditor to record free-text remarks against any audit finding.                                                                                                                                                                                       | Must Have    |
| FR-AUD-12 | The system shall capture a digital signature from the auditor upon audit submission.                                                                                                                                                                                            | Must Have    |
| FR-AUD-13 | The system shall route completed audits to the relevant Department Head for approval before finalization.                                                                                                                                                                       | Must Have    |
| FR-AUD-14 | The system shall generate an Audit Completion Certificate upon final approval of an audit.                                                                                                                                                                                      | Must Have    |
| FR-AUD-15 | The system shall generate an Exception Report listing all Missing and Damaged assets identified in an audit.                                                                                                                                                                    | Must Have    |
| FR-AUD-16 | The system shall provide an Audit Dashboard showing progress (percent verified) for all in-progress audits.                                                                                                                                                                     | Must Have    |
| FR-AUD-17 | The system shall provide Audit Analytics comparing results across audit cycles (e.g., missing-asset trend over time).                                                                                                                                                           | Should Have  |
| FR-AUD-18 | The system shall treat all submitted audit records as immutable; corrections shall be recorded as new, linked entries rather than edits to history, and shall support a legal-hold flag preventing purge of a specific audit's records under retention policy (FR-CMP-01).      | Must Have    |
| FR-AUD-19 | The system shall support an offline scan queue that stores scans locally on the device and synchronizes when connectivity to the on-premises server resumes.                                                                                                                    | Should Have  |
| FR-AUD-20 | For scopes exceeding a configurable asset-count threshold, the system shall support statistically valid sample-based audits (define sample size/method, extrapolate compliance rate) as an alternative to 100% verification, to keep large-scale audits operationally feasible. | Should Have  |
| FR-AUD-21 | The system shall support a Reconciliation workflow: when an asset previously marked Missing in a closed audit is subsequently located, the system shall record the find, link it back to the original exception, and update audit analytics (FR-AUD-17) accordingly.            | Must Have    |
| FR-AUD-22 | The system shall block an Auditor from approving their own submitted audit (FR-USR-06). Where the Auditor and the scoped Department Head are configured as the same individual (permitted only in a small organization operating under an active Separation-of-Duties Waiver, FR-USR-07), the system shall route approval instead to the Super Administrator or a per-organization configured alternate approver, rather than silently allowing self-approval or permanently blocking the audit from ever closing. | Must Have |
| FR-AUD-23 | The expected-asset list for an audit is fixed at audit creation (FR-AUD-03). If an in-scope asset is transferred, reassigned, or has its status changed by another workflow (e.g., FR-LIF-05 Transfer) while the audit is still open, the system shall not silently classify it as Missing at closure; instead it shall flag the asset as "Scope Changed During Audit" with a linked reference to the triggering event, and require the Auditor or Department Head to explicitly disposition it (confirm as verified via the new location, exclude from this audit's scope, or accept as a genuine exception) before the audit can be finalized. | Must Have |

## 2.6 Barcode/QR/RFID Scanning (SCN)

Covers the identification and scanning subsystem, including the labeling standard identified as missing during review.

| **ID**    | **Requirement**                                                                                                                                                                                                                                                           | **Priority** |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-SCN-01 | The system shall support scanning input from USB barcode scanners (keyboard-wedge mode).                                                                                                                                                                                  | Must Have    |
| FR-SCN-02 | The system shall support scanning input from Bluetooth barcode scanners.                                                                                                                                                                                                  | Must Have    |
| FR-SCN-03 | The system shall support scanning via device camera on Android, iPhone, and laptop webcam through the browser.                                                                                                                                                            | Must Have    |
| FR-SCN-04 | The system shall detect and warn on duplicate scans of the same asset within a single audit or scan session.                                                                                                                                                              | Must Have    |
| FR-SCN-05 | The system shall resolve a scanned code to an asset record and display it within 1 second under normal network conditions.                                                                                                                                                | Must Have    |
| FR-SCN-06 | The scanning subsystem shall be designed with an abstraction layer allowing an RFID reader to be added as an additional input source without redesigning the scan workflow.                                                                                               | Should Have  |
| FR-SCN-07 | The system shall generate labels to a defined symbology standard (Code128 for linear barcodes, QR Code with a minimum error-correction level of M) and a defined set of label sizes compatible with common thermal label printers, documented in the Administrator Guide. | Must Have    |

## 2.7 Reporting (RPT)

Covers all standard and ad hoc reports across modules.

| **ID**    | **Requirement**                                                                                                                                     | **Priority** |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-RPT-01 | The system shall provide a full Asset Register report, filterable by category, department, location, and status.                                    | Must Have    |
| FR-RPT-02 | The system shall provide Department, Room, and Building Inventory reports.                                                                          | Must Have    |
| FR-RPT-03 | The system shall provide an Employee Asset List report showing all assets currently assigned to a given person.                                     | Must Have    |
| FR-RPT-04 | The system shall provide Missing Assets, Lost Assets, and Damaged Assets reports sourced from audit findings.                                       | Must Have    |
| FR-RPT-05 | The system shall provide Warranty Expiry, AMC Expiry, and Insurance Expiry reports with configurable lookahead windows.                             | Must Have    |
| FR-RPT-06 | The system shall provide Purchase History and Vendor reports.                                                                                       | Should Have  |
| FR-RPT-07 | The system shall provide an Asset Movement report showing location/assignment changes over a date range.                                            | Must Have    |
| FR-RPT-08 | The system shall provide Audit Compliance and Audit Summary reports across one or more audit cycles, including reconciliation outcomes (FR-AUD-21). | Must Have    |
| FR-RPT-09 | The system shall provide a Depreciation report using the calculation engine defined in FR-AST-15.                                                   | Should Have  |
| FR-RPT-10 | The system shall provide a Maintenance History report.                                                                                              | Must Have    |
| FR-RPT-11 | The system shall support label printing (barcode/QR + asset metadata) in batch for a filtered set of assets, per the standard in FR-SCN-07.         | Must Have    |
| FR-RPT-12 | All reports shall be exportable to PDF, Excel, and CSV, and shall support direct printing.                                                          | Must Have    |
| FR-RPT-13 | The system shall support scheduling of recurring reports for automatic generation and delivery.                                                     | Should Have  |

## 2.8 Dashboard (DSH)

Covers executive/operational dashboards providing at-a-glance visibility.

| **ID**    | **Requirement**                                                                                                   | **Priority** |
|-----------|-------------------------------------------------------------------------------------------------------------------|--------------|
| FR-DSH-01 | The system shall display total asset count, and breakdowns by category, department, building, campus, and status. | Must Have    |
| FR-DSH-02 | The system shall display audit completion percentage for all active audits.                                       | Must Have    |
| FR-DSH-03 | The system shall display upcoming warranty, AMC, and insurance expirations, and maintenance due items.            | Must Have    |
| FR-DSH-04 | The system shall display low-stock and expiry-approaching alerts for inventory items.                             | Must Have    |
| FR-DSH-05 | The system shall display a recent activity feed and an audit calendar view.                                       | Should Have  |
| FR-DSH-06 | The system shall present configurable KPIs using interactive charts.                                              | Should Have  |
| FR-DSH-07 | Dashboard content shall be filtered according to the viewing user's role and organizational scope.                | Must Have    |

## 2.9 User Management & RBAC (USR)

Covers user accounts, roles, permissions, and the offboarding gap identified during review.

| **ID**    | **Requirement**                                                                                                                                                                                                                         | **Priority** |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-USR-01 | The system shall support the following default roles: Super Administrator, Administrator, Inventory Manager, Auditor, Read-only Auditor, Department Head, Volunteer, Viewer, IT Security Officer, Data Protection/Compliance Officer. The latter two were omitted from this list in earlier revisions despite carrying explicit sign-off/escalation authority elsewhere in this FRS and in BRD Sections 6.5 and 14.1 (adversarial review v4.0 fix) — they are core governance roles, not organization-specific customizations, so they belong in the default set rather than being layered on via FR-USR-02's custom-role mechanism. | Must Have    |
| FR-USR-02 | The system shall allow an Administrator to define custom roles with a configurable permission set.                                                                                                                                      | Should Have  |
| FR-USR-03 | The system shall enforce permission checks at both the API and UI layer for every protected action.                                                                                                                                     | Must Have    |
| FR-USR-04 | The system shall allow scoping of a role's access to specific organizational hierarchy nodes (e.g., a Department Head sees only their department/site).                                                                                 | Must Have    |
| FR-USR-05 | The system shall support an Offboarding workflow: deactivating a user shall trigger a mandatory review of any assets currently assigned to them, requiring reassignment or return-to-inventory before the account is fully deactivated. | Must Have    |
| FR-USR-06 | The system shall block a user from being the sole approver of an action on an entity they themselves created, registered, or submitted (e.g., the individual who registered an asset's valuation shall not also record the audit finding that approves it; the individual who initiates a transfer shall not be its sole approver), enforced server-side, not as a UI-only convention. This default may only be overridden per FR-USR-07. | Must Have    |
| FR-USR-07 | The system shall support recording a Separation-of-Duties Waiver at the organization level: who approved it, the date, the scope it covers, and a required IT Security Officer sign-off flag, consistent with BRD Section 2.1/6.5. While an active waiver covers a given action type, FR-USR-06's block is relaxed for that action type only; the system shall log every action taken under an active waiver distinctly from actions taken under normal separation-of-duties enforcement, so a reviewer can identify waiver-covered activity later. | Must Have    |

## 2.10 Notifications (NTF)

Covers proactive alerts delivered to users, including the preference-granularity gap identified during review.

| **ID**    | **Requirement**                                                                                                                                                                                                                   | **Priority** |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-NTF-01 | The system shall send email notifications for audit reminders, warranty/AMC/insurance expiry, inventory expiry, low stock, and pending approvals.                                                                                 | Must Have    |
| FR-NTF-02 | The system shall support optional SMS notifications for the same event types, where an SMS gateway is configured; this is treated as an external integration under FR-INT and Section 6.4 of the BRD.                             | Could Have   |
| FR-NTF-03 | The system shall display in-app notifications with read/unread state.                                                                                                                                                             | Must Have    |
| FR-NTF-04 | The system shall notify relevant users on assignment and transfer of an asset.                                                                                                                                                    | Must Have    |
| FR-NTF-05 | The system shall allow each user to configure per-event-type notification preferences (email / in-app / both / none), subject to any mandatory notifications an Administrator designates as non-optional (e.g., security alerts). | Should Have  |

## 2.11 Search (SRC)

Covers system-wide search capability.

| **ID**    | **Requirement**                                                                                                        | **Priority** |
|-----------|------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-SRC-01 | The system shall provide a global search across assets, employees, vendors, and purchase orders.                       | Must Have    |
| FR-SRC-02 | The system shall support direct lookup by barcode, QR value, serial number, or asset number.                           | Must Have    |
| FR-SRC-03 | The system shall provide advanced search with combinable filters (category, status, location, department, date range). | Must Have    |
| FR-SRC-04 | The system shall allow users to save and re-run frequently used searches.                                              | Should Have  |
| FR-SRC-05 | The system architecture shall reserve a search field for RFID identifiers for future use.                              | Could Have   |

## 2.12 Security (SEC)

Covers authentication, authorization, and audit logging of the system itself. Expanded per review to explicitly cover integration-layer and operational security.

| **ID**    | **Requirement**                                                                                                                                                                                                                                                                                     | **Priority** |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-SEC-01 | The system shall authenticate users via JWT-based session tokens with configurable expiry and refresh.                                                                                                                                                                                              | Must Have    |
| FR-SEC-02 | The system shall optionally integrate with LDAP or Active Directory for authentication, and optionally with an SSO provider (SAML/OIDC).                                                                                                                                                            | Must Have    |
| FR-SEC-03 | The system shall support optional two-factor authentication.                                                                                                                                                                                                                                        | Could Have   |
| FR-SEC-04 | The system shall maintain a complete, immutable log of user activity, including login history and administrative actions.                                                                                                                                                                           | Must Have    |
| FR-SEC-05 | The system shall enforce configurable password policies (minimum length, complexity, expiry, reuse prevention).                                                                                                                                                                                     | Must Have    |
| FR-SEC-06 | The system shall support configurable session timeout and forced re-authentication for sensitive actions (e.g., permission changes, data export).                                                                                                                                                   | Must Have    |
| FR-SEC-07 | The system shall support optional IP-based access restrictions.                                                                                                                                                                                                                                     | Could Have   |
| FR-SEC-08 | The system shall encrypt sensitive data (credentials, personal data, attachments) at rest and in transit.                                                                                                                                                                                           | Must Have    |
| FR-SEC-09 | Every external integration (Section FR-INT) shall authenticate using mutual TLS or signed API credentials; no integration shall use unauthenticated or shared generic credentials.                                                                                                                  | Must Have    |
| FR-SEC-10 | Outbound webhook payloads (FR-INT-04) shall be cryptographically signed (e.g., HMAC) so the receiving system can verify authenticity.                                                                                                                                                               | Must Have    |
| FR-SEC-11 | All secrets (database credentials, LDAP service account, API keys, signing keys) shall be stored in a secrets manager or encrypted configuration store, never in plaintext source or version control.                                                                                               | Must Have    |
| FR-SEC-12 | The system's dependency set (backend and frontend) shall be scanned for known vulnerabilities as part of the build pipeline, with a defined remediation SLA for High/Critical findings.                                                                                                             | Must Have    |
| FR-SEC-13 | The deploying organization shall have access to a documented process for periodic penetration testing or vulnerability assessment of the deployed instance.                                                                                                                                         | Should Have  |
| FR-SEC-14 | Role-based and organizational-scope access control (FR-USR-03, FR-USR-04) shall be enforced identically for requests originating from external integrations as for interactive users — an integration's service account shall carry its own scoped role, never a blanket administrative credential. | Must Have    |

## 2.13 Data Migration & Bulk Import/Export (MIG)

New module addressing the gap where legacy-data migration was assumed in the BRD but never specified as a functional requirement. Per BRD Section 8.2 (v4.0), this module is split across releases: FR-MIG-01, 03, and 04 (import, dry-run/validation, and logging) ship in R1, since an organization cannot go live on R1 without loading its existing spreadsheet data. FR-MIG-02 (bulk export/data-portability) ships in R3 alongside external integrations, once the product has been validated with real data.

| **ID**    | **Requirement**                                                                                                                                                                                                  | **Priority** | **Release** |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|-------------|
| FR-MIG-01 | The system shall support bulk import of asset, employee/volunteer, and vendor records from CSV/Excel templates, with field-level validation and a per-row error report identifying rejected records and reasons. | Must Have    | R1 |
| FR-MIG-02 | The system shall support bulk export of the same entity types, in a format suitable for re-import, to support migration off spreadsheets in stages and to provide a data-portability path off IAMS itself.       | Must Have    | R3 |
| FR-MIG-03 | The system shall support a dry-run/staging import mode that reports what would be created/updated/rejected without committing changes, followed by a reconciliation report once the import is committed.         | Must Have    | R1 |
| FR-MIG-04 | Bulk import/export operations shall be logged (who ran it, when, row counts, outcome) as part of the immutable activity log (FR-SEC-04).                                                                         | Must Have    | R1 |

## 2.14 External Integrations (INT)

New module addressing the gap where accounting/ERP and HR/SIS integration needs were identified but not specified. All integrations are opt-in per BRD Section 6.4 and secured per FR-SEC-09–14.

| **ID**    | **Requirement**                                                                                                                                                                                                                                | **Priority** |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-INT-01 | The system shall support exporting asset valuation and depreciation data in a format consumable by common accounting/ERP systems (e.g., CSV mapped to a documented schema, or a REST API for systems that support pull-based integration).     | Should Have  |
| FR-INT-02 | The system shall support an optional, authenticated integration to synchronize employee, department, or (where applicable) student roster data from an organization's HR or Student Information System (SIS), reducing duplicate manual entry. | Could Have   |
| FR-INT-03 | The system shall support optional SSO integration (SAML 2.0 or OIDC) as an alternative to local/LDAP authentication.                                                                                                                           | Should Have  |
| FR-INT-04 | The system shall support outbound webhooks for key events (audit completed, asset status changed) so an organization can connect its own downstream tooling, subject to FR-SEC-10 signing.                                                     | Could Have   |
| FR-INT-05 | Every integration shall be individually enable/disable-able by an Administrator, and disabled by default until explicitly configured.                                                                                                          | Must Have    |

## 2.15 Compliance & Data Privacy (CMP)

New module addressing the compliance and privacy gaps identified during review (BRD Section 6).

| **ID**    | **Requirement**                                                                                                                                                                                                                                                                                                              | **Priority** |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-CMP-01 | The system shall support a configurable data retention policy per entity type (e.g., login logs, audit evidence, former-employee records), with automated flagging of records eligible for anonymization or deletion at policy expiry.                                                                                       | Must Have    |
| FR-CMP-02 | The system shall support an anonymization workflow for personal data of a departed employee/volunteer (or, in education deployments, a departed student) that removes identifying fields while preserving the integrity of historical asset and audit records (e.g., replacing a name with a stable pseudonymous reference). | Must Have    |
| FR-CMP-03 | The system shall record the basis/notice under which personal data fields were collected, configurable per deployment to match applicable law, for display in a privacy notice generated from configuration.                                                                                                                 | Should Have  |
| FR-CMP-04 | The web application shall conform to WCAG 2.1 Level AA, including keyboard navigability, screen-reader labeling, and sufficient color contrast across all core workflows (registration, audit scanning, reporting).                                                                                                          | Must Have    |
| FR-CMP-05 | The system shall provide an administrative view confirming that all data stores (database, attachments, backups) reside within the organization's designated on-premises infrastructure, with no data replicated to a third-party cloud service unless an integration (FR-INT) is explicitly enabled.                        | Must Have    |
| FR-CMP-06 | The system shall support a legal-hold flag on audit or asset records that overrides the standard retention policy (FR-CMP-01), preventing deletion or anonymization of held records until the hold is lifted by an authorized role.                                                                                          | Must Have    |
| FR-CMP-07 | Depreciation and valuation calculations (FR-AST-15) shall support methods consistent with common financial reporting standards (GAAP/IFRS-aligned straight-line and declining-balance) so outputs are usable for statutory reporting.                                                                                        | Should Have  |

## 2.16 Product Analytics (ANL)

New module addressing the review finding that no usage-analytics or product-feedback mechanism existed to inform post-launch prioritization (BRD Section 15, BR-20).

| **ID**    | **Requirement**                                                                                                                                                                                                                          | **Priority** |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| FR-ANL-01 | The system shall record local, aggregated feature-usage metrics (e.g., which modules and report types are actually opened, audit types used) without capturing personal data or document content in the metrics store.                   | Should Have  |
| FR-ANL-02 | Usage metrics shall remain entirely within the organization's own database — no metrics shall be transmitted outside the on-premises deployment unless the organization explicitly opts into a future centralized reporting integration. | Must Have    |
| FR-ANL-03 | The system shall provide an Administrator-facing usage report (feature adoption by role, module usage frequency) to support the quarterly review defined in BRD Section 15.                                                              | Should Have  |
| FR-ANL-04 | The system shall provide an in-app feedback submission form (free text plus category) available to all authenticated roles, routed to a configurable recipient (e.g., the organization's Product Owner or Administrator).                | Should Have  |

# 3. Traceability

Each functional requirement in this document traces back to one or more business requirements (BR-xx) defined in the BRD, and forward to the epics, user stories, and API endpoints to be defined in the SRS and subsequent technical artifacts. Compliance-related requirements (module CMP) trace specifically to BRD Section 6 (Regulatory and Compliance Requirements); integration and migration requirements (modules INT, MIG) trace to BR-15 and BR-16; product analytics requirements (module ANL) trace to BR-20 and BRD Section 15 (Product Feedback and Continuous Discovery). A full traceability matrix will be maintained as a living artifact once the backlog is created.

# 4. Acceptance Criteria Approach

Detailed Given/When/Then acceptance criteria are defined at the user story level once the product backlog is created. This FRS defines requirement scope and priority; the SRS defines the non-functional and security envelope; user stories will define testable acceptance criteria for each FR, including explicit test cases for the compliance requirements in module CMP (e.g., a test verifying anonymization does not corrupt historical audit totals).

# 5. Release Mapping (R1 / R2 / R3)

Added in v4.0 to fix a sequencing gap: BRD Section 8.2 maps releases at the Business Requirement (BR) level, but several BRs bundle FRs of very different weight and dependency (e.g., BR-08 Reporting and BR-15 Migration each span a "basic" and a "full" scope that land in different releases). This section is the authoritative FR-to-release mapping engineering should plan sprints against; where it's silent for a given FR, that FR follows its parent BR's release in BRD Section 8.2 with no split.

| **Release** | **FRs (explicitly split from their parent BR's default release)** | **Basis** |
|---|---|---|
| R1 | FR-RPT-01, FR-RPT-03, FR-RPT-12 (basic reporting, out of BR-08) | BRD 8.2 — R1 must produce a basic asset register |
| R1 | FR-MIG-01, FR-MIG-03, FR-MIG-04 (import/dry-run/logging, out of BR-15) | BRD 8.2 — R1 must be able to load existing spreadsheet data |
| R1 | FR-USR-06, FR-USR-07 (SoD enforcement + waiver, new in v4.0, part of BR-21) | BR-21 is R1 — self-approval blocking must exist from go-live, not retrofitted |
| R1 | FR-INT-03 (SSO/SAML2/OIDC authentication, out of module INT, otherwise R3 alongside BR-16) | BR-07 ("RBAC... and optional SSO integration") is explicitly R1 in BRD 8.2 — an organization needing SSO from day one cannot wait for the rest of module INT (accounting/HR sync, webhooks) to ship in R3. Only the authentication path of module INT ships early; the data-integration endpoints (FR-INT-01/02/04) remain R3 |
| R2 | All other FR-RPT-\* and FR-DSH-\* (full reporting/dashboard suite, out of BR-08) | BRD 8.2 — deferred once basic reporting is proven in R1 |
| R2 | FR-AUD-22, FR-AUD-23 (self-approval block + scope-integrity, new in v4.0, part of BR-05/BR-22) | Only meaningful once the Audit module (R2) exists |
| R3 | FR-MIG-02 (bulk export/data-portability, out of BR-15) | BRD 8.2 — deferred alongside external integrations |

All other FRs not listed above (the large majority — AST, INV, ORG, LIF core flows, SCN, SEC, remaining CMP and ANL requirements, etc.) follow the release of their parent BR exactly as mapped in BRD Section 8.2 with no further split.
