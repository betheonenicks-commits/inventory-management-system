**User Personas, Roles & Use Cases**

**Inventory Audit Management System (IAMS)**

*Persona Profiles, Role Responsibilities, and Capability-Level Use Cases*

Document ID: IAMS-PUC-1.0 \| Version: 1.0 \| Status: Draft for Review \| Date: July 2026

# Table of Contents

*(Right-click and select “Update Field” in Word to populate this table after opening the document.)*

# Document Control

## Purpose and Relationship to Other Documents

This document companions the BRD (IAMS-BRD-3.0), FRS (IAMS-FRS-3.0), and SRS (IAMS-SRS-3.0). It answers a question those three deliberately leave implicit: who actually uses this system, in what capacity, and what does using it look like end to end? Every persona below maps to a role defined in BRD Section 2 (Stakeholders) and FRS FR-USR-01; every use case maps to one or more functional requirements in the FRS, referenced by ID so a developer or tester can trace a concrete user scenario back to its formal requirement.

| **Version** | **Date**   | **Author**         | **Description**                                                                                                  |
|-------------|------------|--------------------|------------------------------------------------------------------------------------------------------------------|
| 1.0         | 2026-07-06 | Product Management | Initial persona set, roles & responsibilities matrix, and capability-level use cases covering all 16 FRS modules |

# 1. User Personas

Nine personas cover every role defined in the BRD stakeholder table. Each persona reflects a real behavioral pattern likely across the target sectors (education, church/non-profit, corporate) rather than a sector-specific caricature — consistent with the BRD's sector-agnostic design principle.

**Priya — The Super Administrator**

*“I own the whole system, and if something's misconfigured, that's on me.”*

**Role**

Super Administrator

**Context**

IT lead or senior operations staff, one per deployment, moderate-to-high technical proficiency

**Goals**

- Keep the system configured correctly for the organization's actual structure

- Ensure security policy and access control are correctly enforced

- Avoid being the single point of failure for day-to-day requests

**Pain Points**

- Fear of misconfiguring something that silently breaks reporting or access control

- Limited time — often wears multiple IT hats

**Primary Modules**

ORG, USR, SEC, INT, CMP, MIG

**Marcus — The Administrator**

*“I keep the day-to-day running: users, configuration, cross-department requests.”*

**Role**

Administrator

**Context**

Office manager or operations coordinator, moderate technical proficiency, handles requests from multiple departments

**Goals**

- Onboard/offboard users quickly and correctly

- Resolve configuration requests without escalating to IT

- Keep custom fields and categories aligned with how the organization actually works

**Pain Points**

- Gets pulled in when a Department Head can't find something

- Needs confidence that offboarding an employee won't silently orphan their assigned assets

**Primary Modules**

USR, ORG, AST, NTF, ANL

**Elena — The Inventory Manager**

*“If it's physical and it belongs to us, I need to know where it is and what it's worth.”*

**Role**

Inventory Manager

**Context**

Facilities, procurement, or operations staff; the heaviest day-to-day user of the Asset and Inventory modules

**Goals**

- Register new assets quickly at intake, with labels ready to print

- Keep stock levels accurate across multiple warehouses

- Track warranty, AMC, and insurance so nothing lapses unnoticed

**Pain Points**

- Manual spreadsheet reconciliation after every delivery

- No visibility into which department actually has a given asset today

**Primary Modules**

AST, INV, LIF, RPT

**Devon — The Auditor**

*“I'm the one actually walking the building with a scanner. Make that part fast.”*

**Role**

Auditor

**Context**

Staff member or trained volunteer conducting physical verification; mobile-first, often low-to-moderate technical proficiency, works standing/walking rather than at a desk

**Goals**

- Get through an audit scope quickly without re-typing anything

- Flag problems (missing, damaged) with evidence in the moment, not from memory later

- Trust that a scan actually registered before moving to the next asset

**Pain Points**

- Poor mobile connectivity in basements/storage rooms

- Paper-based audits today mean findings get transcribed hours later, with details forgotten

**Primary Modules**

AUD, SCN, SRC

**Grace — The Read-only Auditor**

*“I review what happened. I don't touch anything.”*

**Role**

Read-only Auditor

**Context**

External or compliance-focused reviewer (e.g., a diocesan finance reviewer, an external statutory auditor for a non-profit), infrequent user, needs to trust the data is unaltered

**Goals**

- Confirm audit evidence and findings are complete and haven't been retroactively edited

- Produce or review compliance reports without needing IT support

**Pain Points**

- Needs assurance the immutable audit trail (FR-AUD-18) actually is immutable, not just labeled as such

**Primary Modules**

AUD, RPT, DSH

**Father Thomas / Dr. Osei — The Department Head**

*“I need to approve things for my area without becoming a full-time system user.”*

**Role**

Department Head (Ministry Lead / Dean / Department Manager depending on sector)

**Context**

Occasional user, approves rather than operates; wants fast, mobile-friendly approval screens; often not tech-savvy by preference, not by inability

**Goals**

- Approve or reject transfers and audit sign-offs quickly

- See a simple view of what's assigned to their department without digging

**Pain Points**

- Doesn't want to “learn a system” — wants approvals to feel like a two-tap action

**Primary Modules**

LIF, AUD, DSH, NTF

**Sam — The Employee / Volunteer**

*“I just want to know what's assigned to me and get notified if something needs my attention.”*

**Role**

Employee / Volunteer

**Context**

The largest population of users by headcount, lowest average system engagement, wide range of technical proficiency

**Goals**

- See what's currently assigned to them

- Acknowledge assignment/transfer without confusion

- Control which notifications they actually receive

**Pain Points**

- Doesn't want to be nagged by notifications for things irrelevant to them

- May be a volunteer with only occasional system access

**Primary Modules**

LIF, NTF, ANL (feedback)

**Board Member / Finance Officer — The Viewer**

*“I want the numbers, on demand, without asking anyone to pull them for me.”*

**Role**

Viewer / Finance & Accounting Office

**Context**

Executive, board member, or finance staff; read-only, report-and-dashboard-only usage; infrequent but high-visibility use (e.g., board meetings, audit season)

**Goals**

- Pull an accurate asset register or depreciation report without developer or IT help

- Trust the numbers are current and traceable

**Pain Points**

- Historically dependent on someone else compiling a spreadsheet, often under time pressure before a board meeting

**Primary Modules**

RPT, DSH, INT

**Officer Reyes — The IT Security / Compliance Officer**

*“I need to know this system won't be the reason we're in the news for a breach.”*

**Role**

IT Security Officer / Data Protection & Compliance Officer (distinct roles, grouped here as they share a governance-oriented usage pattern)

**Context**

IT security specialist or compliance-focused administrator; reviews rather than operates day to day; technically proficient, security-literate

**Goals**

- Verify access logs and security posture before sign-off

- Confirm retention policy, anonymization, and legal-hold controls are actually enforced, not just documented

**Pain Points**

- Needs evidence, not assurances — wants to see the log, not be told the log exists

**Primary Modules**

SEC, CMP, INT (security review)

# 2. Roles and Responsibilities Matrix

This matrix expands the BRD Section 2 stakeholder table into concrete responsibilities and approval authority, and states the separation-of-duties boundary required by BRD Section 2.1.

| **Role**                             | **Key Responsibilities**                                                                                                              | **Approval Authority**                                                                                                             |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| Super Administrator                  | System configuration; organizational hierarchy setup; security policy; integration enable/disable; final data-retention configuration | Overrides any role's access in an emergency (logged); approves new integrations jointly with Compliance Officer                    |
| Administrator                        | User provisioning/offboarding; day-to-day configuration (categories, custom fields); cross-department coordination                    | Approves standard user role assignment; cannot alter security policy or retention rules                                            |
| Inventory Manager                    | Asset and inventory registration; vendor/warehouse management; label generation; warranty/AMC/insurance tracking                      | Approves stock adjustments and inter-warehouse transfers; does not approve audits on assets they registered (separation of duties) |
| Auditor                              | Executes audits; scans and verifies assets; records findings, evidence, and remarks; submits for approval                             | Submits audit for Department Head approval; cannot self-approve their own audit                                                    |
| Read-only Auditor                    | Reviews audit evidence and findings; produces compliance summaries                                                                    | No write access; no approval authority                                                                                             |
| Department Head                      | Approves transfers and audit completions for their department/site; reviews department-level reports                                  | Final approval authority for audits and transfers scoped to their organizational node                                              |
| Employee / Volunteer                 | Acknowledges assignment/transfer of assets to themselves; manages own notification preferences                                        | None — no approval authority                                                                                                       |
| Viewer                               | Views dashboards and reports within their permitted scope                                                                             | None — read-only                                                                                                                   |
| IT / Infrastructure Team             | Deployment, backups, upgrades, LDAP/AD and SSO configuration                                                                          | Approves infrastructure/security-relevant configuration changes jointly with IT Security Officer                                   |
| Data Protection / Compliance Officer | Owns retention policy, privacy notice content, anonymization approvals, legal-hold authorization, integration data-flow review        | Must sign off before go-live and before any new integration is enabled (BRD Section 6.5, 14.1)                                     |
| IT Security Officer                  | Security review, vulnerability management, incident response ownership                                                                | Must sign off before go-live; can block go-live pending remediation (BRD Section 14.1)                                             |

## 2.1 Role Inheritance and Least Privilege

Roles do not inherit permissions from one another implicitly (e.g., Administrator is not simply “Inventory Manager plus more”) — each role's permission set is defined explicitly per FR-USR-01/02, and a user needing multiple capabilities is assigned multiple roles rather than being granted an overbroad single role. This keeps the separation-of-duties boundary in Section 2 enforceable rather than nominal.

# 3. Capability Use Cases

Use cases are organized by FRS module (Section numbers below correspond to FRS Section 2.x) and reference the specific functional requirements they exercise, so a single use case can be traced directly to acceptance criteria during test design.

## 3.1 Asset Management (AST)

**UC-AST-01 — Register a New Asset and Generate Its Label**

**Primary Actor:** Elena (Inventory Manager) **Related Requirement(s):** FR-AST-01, FR-AST-02, FR-AST-05, FR-AST-06

**Precondition**

Elena has received a new laptop from a vendor and has its purchase order reference on hand.

**Trigger**

Elena opens the Asset Management module and selects “Register New Asset.”

**Main Flow**

1. Elena selects the asset category “IT Equipment,” which surfaces the custom fields defined for that category.

2. Elena enters manufacturer, purchase cost, purchase order reference, and vendor.

3. Elena uploads a photo of the asset and attaches the vendor invoice PDF.

4. The system generates a unique asset number and a corresponding QR code.

5. Elena prints the label directly to a connected thermal label printer.

6. The system records the registration event with Elena's identity and a timestamp in the asset's history.

**Alternate / Exception Flows**

- If a required custom field for the category is left blank, the system blocks submission and highlights the missing field.

- If the label printer is unreachable, the system still completes registration and allows label reprinting later from the asset record.

**Postcondition**

The asset exists in the system with a unique identifier, is labeled, and is ready for assignment or audit inclusion.

## 3.2 Inventory Management (INV)

**UC-INV-01 — Transfer Stock Between Warehouses**

**Primary Actor:** Elena (Inventory Manager) **Related Requirement(s):** FR-INV-03, FR-INV-08

**Precondition**

Elena needs to move 50 units of a consumable from the Main Warehouse to the Annex Storage Room ahead of a scheduled event.

**Trigger**

Elena identifies a shortfall at the Annex during routine stock review.

**Main Flow**

1. Elena opens the inventory item and selects “Transfer.”

2. Elena specifies source warehouse, destination warehouse, and quantity.

3. The system validates that the source warehouse has sufficient quantity on hand.

4. Elena confirms the transfer; the system decrements the source and increments the destination atomically.

5. The system logs the transfer as a linked pair of records (source-out, destination-in), not two independent adjustments.

**Alternate / Exception Flows**

- If the source warehouse has insufficient quantity, the system blocks the transfer and shows the actual available quantity.

**Postcondition**

Stock levels are accurate at both warehouses, and the transfer is traceable as a single logical event.

## 3.3 Organization Management (ORG)

**UC-ORG-01 — Configure the Organizational Hierarchy for a New Site**

**Primary Actor:** Priya (Super Administrator) **Related Requirement(s):** FR-ORG-01, FR-ORG-02, FR-ORG-05

**Precondition**

The organization has opened a new campus/branch that needs to be represented in IAMS.

**Trigger**

Priya is notified during Phase 0 discovery that a new site is being added.

**Main Flow**

1. Priya navigates to Organization Setup and adds a new top-level node under the existing hierarchy type (e.g., a new “Campus”).

2. Priya adds Buildings, Floors, and Rooms beneath the new node, relabeling level names if this organization uses different terminology than the default.

3. Priya assigns a Department Head persona to the new site's top node.

4. The new node becomes available for asset registration, audits, and role scoping.

**Alternate / Exception Flows**

- If Priya attempts to delete a node that still has assets scoped to it, the system blocks deletion and lists the dependent assets.

**Postcondition**

The new site is fully represented in the hierarchy and usable by every other module without additional configuration.

## 3.4 Asset Lifecycle Management (LIF)

**UC-LIF-01 — Approve an Asset Transfer Request**

**Primary Actor:** Father Thomas (Department Head) **Related Requirement(s):** FR-LIF-05

**Precondition**

Elena has initiated a transfer of a projector from Room 101 to Room 204, both within Father Thomas's department.

**Trigger**

Father Thomas receives a notification that a transfer is pending his approval.

**Main Flow**

1. Father Thomas opens the notification, which deep-links to the pending transfer.

2. He reviews the asset, source, and destination in a single screen.

3. He taps Approve.

4. The system updates the asset's location and records the approval, approver identity, and timestamp in the asset's history.

**Alternate / Exception Flows**

- If Father Thomas taps Reject, he is prompted for a brief reason, which is recorded and visible to Elena.

**Postcondition**

The asset's location is updated (or the transfer is closed as rejected) with a complete, attributable record.

**UC-LIF-02 — Process Asset Retirement and Disposal**

**Primary Actor:** Elena (Inventory Manager) **Related Requirement(s):** FR-LIF-09

**Precondition**

A laptop has failed a repair attempt and is beyond economical repair.

**Trigger**

Elena determines the asset should be retired following an unsuccessful repair.

**Main Flow**

1. Elena opens the asset and selects “Retire / Dispose.”

2. She selects a disposition reason (e.g., “Beyond Repair”) and a disposal method, including e-waste-compliant disposal where applicable.

3. She records the disposal date and any recovery value.

4. The system requires Administrator approval before the asset status finalizes as “Disposed.”

5. Once approved, the asset is excluded from future audit scopes but remains visible in historical reports.

**Alternate / Exception Flows**

- If the asset has an open insurance claim, the system warns Elena and requires the claim to be resolved or explicitly acknowledged first.

**Postcondition**

The asset is marked Disposed with a complete disposition record, and no longer appears in active audit scopes.

## 3.5 Audit Management (AUD)

**UC-AUD-01 — Conduct a Quarterly Department Audit via Continuous Scan**

**Primary Actor:** Devon (Auditor) **Related Requirement(s):** FR-AUD-01, FR-AUD-04, FR-AUD-05, FR-AUD-07

**Precondition**

A quarterly audit has been scheduled and assigned to Devon for the Science Department.

**Trigger**

Devon opens the audit on his phone and taps “Start Scanning.”

**Main Flow**

1. The system loads the expected-asset list for the Science Department scope.

2. Devon enables Continuous Scan Mode and walks the department, scanning each asset's QR code.

3. Each successful scan is confirmed with a brief visual/haptic cue, without interrupting the scan flow.

4. The Expected vs. Verified counter updates in real time as Devon progresses.

5. Devon completes the walk-through and reviews the remaining unverified assets.

**Alternate / Exception Flows**

- If Devon scans an asset already verified in this audit, the system warns of a duplicate scan and does not double-count it.

- If a scanned code doesn't resolve to any known asset, the system flags it as an unrecognized scan for later review rather than silently discarding it.

**Postcondition**

All scannable assets in the department are verified or explicitly left outstanding, with full attribution to Devon and a timestamp.

**UC-AUD-02 — Flag and Document a Damaged Asset During Audit**

**Primary Actor:** Devon (Auditor) **Related Requirement(s):** FR-AUD-09, FR-AUD-10, FR-AUD-11

**Precondition**

During the walk-through, Devon finds a projector with a cracked lens.

**Trigger**

Devon scans the damaged projector's QR code.

**Main Flow**

1. The system resolves the scan and shows the asset record.

2. Devon selects condition “Major Damage” from the configurable condition scale.

3. Devon takes a photo of the crack directly within the audit screen.

4. Devon adds a short remark describing how the damage was likely caused.

5. The finding is saved against the audit and the asset's history simultaneously.

**Alternate / Exception Flows**

- If Devon has no signal at the moment of the finding, the finding (including the photo) queues in the offline scan queue and syncs once connectivity returns.

**Postcondition**

The audit's exception report will include this asset with photo evidence and a remark, ready for Department Head review.

**UC-AUD-03 — Review and Approve Audit Completion**

**Primary Actor:** Father Thomas (Department Head) **Related Requirement(s):** FR-AUD-12, FR-AUD-13, FR-AUD-14, FR-AUD-15

**Precondition**

Devon has submitted the completed Science Department audit, digitally signed.

**Trigger**

Father Thomas receives a notification that an audit is awaiting his approval.

**Main Flow**

1. Father Thomas opens the audit summary: Expected vs. Verified counts, and the Exception Report listing the damaged projector and two missing assets.

2. He reviews the photo evidence for the damaged projector.

3. He approves the audit.

4. The system generates a signed Audit Completion Certificate and finalizes the audit as immutable.

**Alternate / Exception Flows**

- If Father Thomas has concerns about a specific finding, he can request clarification from Devon before approving, which pauses finalization without discarding the submitted data.

**Postcondition**

The audit is closed, certified, and immutable; missing/damaged assets are available in the next dashboard refresh and reporting cycle.

**UC-AUD-04 — Reconcile a Previously Missing Asset**

**Primary Actor:** Devon (Auditor) **Related Requirement(s):** FR-AUD-21

**Precondition**

A projector marked “Missing” in last quarter's audit has been found in a different building's storage closet.

**Trigger**

Devon scans the found projector's QR code outside of a scheduled audit.

**Main Flow**

1. The system recognizes the asset was last marked Missing in a closed audit and prompts Devon to reconcile it.

2. Devon confirms the find, records its actual current location, and adds a note.

3. The system links this reconciliation record back to the original missing-asset exception.

4. Audit analytics update to reflect the reconciled status for trend reporting.

**Alternate / Exception Flows**

- If the asset's condition has also changed (e.g., it's now damaged), Devon can record a condition update as part of the same reconciliation.

**Postcondition**

The asset's status is corrected, the original exception is closed with a linked resolution, and historical audit accuracy is preserved rather than silently edited.

## 3.6 Barcode/QR/RFID Scanning (SCN)

**UC-SCN-01 — Scan Assets Using a Mobile Camera in the Field**

**Primary Actor:** Devon (Auditor) **Related Requirement(s):** FR-SCN-03, FR-SCN-04, FR-SCN-05

**Precondition**

Devon does not have a dedicated barcode scanner available for today's audit.

**Trigger**

Devon opens the audit on his phone's browser and taps the camera-scan icon.

**Main Flow**

1. The browser requests camera permission (granted previously or granted now).

2. Devon points the camera at each asset's QR code.

3. The system decodes the code client-side and resolves it to an asset within one second.

4. Devon continues to the next asset without manual data entry.

**Alternate / Exception Flows**

- If Devon scans the same code twice in a row (e.g., camera re-triggers), the duplicate-detection logic (FR-SCN-04) suppresses the double count.

**Postcondition**

Devon completes the audit walk-through using only his phone, with no dedicated hardware required.

## 3.7 Reporting (RPT)

**UC-RPT-01 — Generate and Export the Asset Register Report**

**Primary Actor:** Finance Officer (Viewer) **Related Requirement(s):** FR-RPT-01, FR-RPT-12

**Precondition**

The board meeting is in two days and the Finance Officer needs a current asset register.

**Trigger**

The Finance Officer logs in and navigates to Reports \> Asset Register.

**Main Flow**

1. She filters by category and location to match the board's area of interest.

2. She reviews the on-screen summary totals.

3. She exports the filtered report to Excel.

4. She opens the downloaded file to confirm the figures before the meeting.

**Alternate / Exception Flows**

- If the filtered result set is very large, the export runs as a background job and she is notified when the file is ready, rather than the browser timing out.

**Postcondition**

The Finance Officer has an accurate, exportable asset register without needing to ask IT or Elena to compile it manually.

## 3.8 Dashboard (DSH)

**UC-DSH-01 — Monitor Audit Progress via the Audit Dashboard**

**Primary Actor:** Marcus (Administrator) **Related Requirement(s):** FR-DSH-02

**Precondition**

Three department audits are running concurrently this week.

**Trigger**

Marcus opens the Audit Dashboard from the main navigation.

**Main Flow**

1. Marcus sees all three audits with live Expected vs. Verified completion percentages.

2. He notices one audit has stalled at 40% for two days.

3. He clicks into that audit to see it's assigned to an auditor who's been on leave.

4. He reassigns the audit to another available auditor directly from the dashboard.

**Alternate / Exception Flows**

- If an audit's deadline has passed without completion, the dashboard visually flags it as overdue rather than showing it identically to on-track audits.

**Postcondition**

Marcus catches and resolves a stalled audit before it affects the quarterly compliance report.

## 3.9 User Management & RBAC (USR)

**UC-USR-01 — Offboard a Departing Employee and Recover Assigned Assets**

**Primary Actor:** Marcus (Administrator) **Related Requirement(s):** FR-USR-05

**Precondition**

An employee has resigned and their last day is tomorrow.

**Trigger**

Marcus initiates deactivation of the employee's account.

**Main Flow**

1. The system detects three assets currently assigned to the employee and blocks immediate deactivation.

2. Marcus is shown the list: a laptop, a phone, and a company vehicle key fob.

3. Marcus reassigns the laptop and phone to inventory (“returned, awaiting reissue”) and transfers the vehicle key fob to a coworker.

4. With no outstanding assignments, the system allows Marcus to complete deactivation.

**Alternate / Exception Flows**

- If Marcus attempts to deactivate without resolving assignments, the system explains exactly which assets are blocking deactivation rather than giving a generic error.

**Postcondition**

No asset is left silently assigned to a departed employee; the audit trail shows exactly when and how each asset was recovered.

## 3.10 Notifications (NTF)

**UC-NTF-01 — Configure Personal Notification Preferences**

**Primary Actor:** Sam (Employee/Volunteer) **Related Requirement(s):** FR-NTF-05

**Precondition**

Sam is getting too many email notifications for assignment changes he doesn't need to act on.

**Trigger**

Sam opens his profile settings and selects Notification Preferences.

**Main Flow**

1. Sam sees a list of notification types (assignment, transfer, approval requests) with per-type toggle for Email / In-App / Both / None.

2. Sam switches assignment notifications to “In-App only.”

3. Sam saves the preference.

**Alternate / Exception Flows**

- If an Administrator has marked a notification type as mandatory (e.g., security alerts), that toggle is shown as locked with an explanation rather than silently ignored.

**Postcondition**

Sam receives only the notifications he actually wants, without losing any notification an Administrator has designated as mandatory.

## 3.11 Search (SRC)

**UC-SRC-01 — Locate an Asset via Global Search During an Audit**

**Primary Actor:** Devon (Auditor) **Related Requirement(s):** FR-SRC-01, FR-SRC-02

**Precondition**

Devon needs to find a specific asset by its serial number, which isn't in his current audit scope.

**Trigger**

Devon types the serial number into the global search bar.

**Main Flow**

1. The system matches the serial number directly to an asset record.

2. Devon opens the record to confirm its expected location.

3. He navigates to that location to physically verify it.

**Alternate / Exception Flows**

- If no exact match is found, the system offers the closest partial matches rather than a bare “no results” message.

**Postcondition**

Devon locates the correct physical asset without browsing the full asset list.

## 3.12 Security (SEC)

**UC-SEC-01 — Investigate a Suspicious Login via Activity Logs**

**Primary Actor:** Officer Reyes (IT Security Officer) **Related Requirement(s):** FR-SEC-04

**Precondition**

An unusual number of failed login attempts were reported for one account.

**Trigger**

Officer Reyes opens the Security Activity Log.

**Main Flow**

1. She filters by the affected username and the relevant date range.

2. She reviews the sequence of failed attempts, source IP pattern, and the eventual outcome (lockout or successful login).

3. She determines whether to force a password reset and/or apply an IP restriction.

4. She documents her findings and closes the investigation.

**Alternate / Exception Flows**

- If the log shows a successful login following the failed attempts from a new/unrecognized location, she escalates per the incident response procedure (SRS Section 6.9) rather than closing the investigation.

**Postcondition**

The suspicious activity is investigated with a complete, immutable log trail, and appropriate action is taken and documented.

## 3.13 Data Migration & Bulk Import/Export (MIG)

**UC-MIG-01 — Bulk Import Legacy Asset Data from Spreadsheet**

**Primary Actor:** Priya (Super Administrator) **Related Requirement(s):** FR-MIG-01, FR-MIG-03

**Precondition**

The organization has a 3,000-row spreadsheet of existing assets to bring into IAMS before go-live.

**Trigger**

Priya downloads the import template and maps her spreadsheet's columns to it.

**Main Flow**

1. Priya uploads the file in dry-run mode.

2. The system reports 2,940 rows valid, 60 rows with errors (e.g., invalid category names), without committing anything.

3. Priya corrects the 60 rows in her spreadsheet and re-runs the dry run until zero errors remain.

4. Priya commits the import.

5. The system generates a reconciliation report confirming 3,000 assets created, with their new asset numbers and generated labels queued for printing.

**Alternate / Exception Flows**

- If Priya closes the browser mid-import, the import job continues server-side and she can check its status when she returns.

**Postcondition**

All legacy assets exist in IAMS with unique identifiers, and Priya has a reconciliation report proving nothing was lost or duplicated.

## 3.14 External Integrations (INT)

**UC-INT-01 — Export Depreciation Data to the Accounting System**

**Primary Actor:** Finance Officer (Viewer / Finance & Accounting Office) **Related Requirement(s):** FR-INT-01

**Precondition**

Fiscal year-end reporting requires current depreciation figures in the organization's accounting system.

**Trigger**

The Finance Officer requests the depreciation export from Priya, since enabling any integration requires Super Administrator and Compliance Officer sign-off.

**Main Flow**

1. Priya confirms the accounting export integration is enabled and scoped read-only.

2. The Finance Officer runs the depreciation export for the fiscal year.

3. The system produces a file mapped to the organization's accounting import schema.

4. The Finance Officer imports the file into the accounting system and reconciles totals.

**Alternate / Exception Flows**

- If the integration is not yet enabled, the system clearly states it must be configured by an Administrator rather than failing silently.

**Postcondition**

Depreciation figures are available in the accounting system without manual recalculation, with the data flow limited to read-only financial data only.

## 3.15 Compliance & Data Privacy (CMP)

**UC-CMP-01 — Anonymize a Departed Volunteer's Personal Data**

**Primary Actor:** Officer Reyes (Compliance Officer) **Related Requirement(s):** FR-CMP-01, FR-CMP-02

**Precondition**

A volunteer who left the organization two years ago is now eligible for anonymization under the retention policy.

**Trigger**

The system's retention engine flags the volunteer's record as eligible during its periodic review.

**Main Flow**

1. Officer Reyes reviews the flagged record and confirms no legal hold applies.

2. She approves anonymization.

3. The system replaces identifying fields (name, contact details) with a stable pseudonymous reference, while preserving the historical asset assignment and audit records that reference that reference.

4. The anonymization action is itself logged immutably.

**Alternate / Exception Flows**

- If a legal-hold flag is present on any record referencing this volunteer, the system blocks anonymization for that record and explains why.

**Postcondition**

The volunteer's personal data is anonymized in compliance with the retention policy, without corrupting historical asset/audit totals.

## 3.16 Product Analytics (ANL)

**UC-ANL-01 — Review Feature Usage Report and Submit Feedback**

**Primary Actor:** Marcus (Administrator, acting as local Product Owner) **Related Requirement(s):** FR-ANL-03, FR-ANL-04

**Precondition**

It's the end of the quarter, and Marcus is due to review usage patterns per BRD Section 15.

**Trigger**

Marcus opens the Usage Report from the Analytics section.

**Main Flow**

1. He sees that the Bulk Audit feature has near-zero usage, while Department Audits are used heavily.

2. He submits an in-app feedback note flagging that Bulk Audit's workflow may be confusing rather than unnecessary, based on a conversation with Devon.

3. The feedback is routed to the designated Product Owner recipient for the quarterly triage.

**Alternate / Exception Flows**

- If usage data shows a feature is entirely unused organization-wide for two consecutive quarters, the report highlights it as a candidate for the roadmap re-evaluation in BRD Section 15.

**Postcondition**

Real usage evidence, not assumption, feeds the next roadmap prioritization cycle.

# 4. Traceability Summary

Every persona in Section 1 maps to a role in FRS FR-USR-01 and BRD Section 2. Every use case in Section 3 references at least one FRS requirement ID, and every FRS module (AST through ANL, 16 modules total) has at least one corresponding use case above. This document does not introduce new functional scope — it illustrates the existing FRS scope through the lens of the people who will actually use it, and should be used alongside the FRS during backlog creation to write Given/When/Then acceptance criteria per user story.
