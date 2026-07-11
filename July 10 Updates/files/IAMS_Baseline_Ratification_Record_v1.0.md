# Baseline Ratification Record
## Inventory Audit Management System (IAMS) — Consolidated Baseline v2.0

**Document ID:** IAMS-RATIFY-1.0 | **Status:** Awaiting Signatures | **Date Prepared:** 2026-07-10

> **What this document is:** a formal record that the named stakeholders reviewed and accept the consolidated requirements baseline as the single source of truth for R1 development. **What this document is not:** a substitute for that review. No signature on this page is valid until the named person has actually read the cited document(s) for their row and checked the box themselves. This file should be re-saved (or printed and scanned) with initials/signatures filled in — nothing below is pre-completed.

---

## 1. What Is Being Ratified

The following documents, all dated 2026-07-10, together form **the single authoritative requirements baseline** for IAMS. They supersede every prior draft (BRD v1.0, "Client Ready BRD," any document referencing "BRD-3.0" or "BRD-4.0," FRS/SRS v1.2, PUC v1.0, DD v1.0 standalone, API v1.0 standalone):

| # | Document | ID | Location |
|---|---|---|---|
| 1 | Business Requirements Document | IAMS-BRD-2.0 | `IAMS_BRD_v2.0.md` |
| 2 | Functional Requirements Specification | IAMS-FRS-2.0 | `IAMS_FRS_v2.0.md` |
| 3 | Software Requirements Specification | IAMS-SRS-2.0 | `IAMS_SRS_v2.0.md` |
| 4 | Data Dictionary Amendment | IAMS-DD-1.1 | `IAMS_Data_Dictionary_v1.1_Amendment.md` |
| 5 | API Specification | IAMS-API-1.1 | `IAMS_API_Specification_v1.1.md` |
| 6 | Acceptance Criteria (R1) | IAMS-AC-1.0 | `IAMS_Acceptance_Criteria_R1.md` |
| 7 | Personas & Use Cases | IAMS-PUC-1.1 | `IAMS_Personas_and_Use_Cases_v1.1.md` |
| 8 | Frontend/UX Design Specification | IAMS-FUX-1.1 | `IAMS_Frontend_UX_Design_Specification_v1.1.md` |
| 9 | Backend Architecture Specification | IAMS-BAS-1.1 | `IAMS_Backend_Architecture_Specification_v1.1.md` |
| 10 | Middleware/Infrastructure/Security Specification | IAMS-MIS-1.1 | `IAMS_Middleware_Infrastructure_Security_Specification_v1.1.md` |
| 11 | Epics & User Stories | IAMS-EUS-1.0 | `IAMS_Epics_and_User_Stories_v1.0.md` |
| 12 | Requirements Traceability Matrix & Readiness Report | IAMS-RTM-1.1 / RTM-REPORT-1.1 | `IAMS_RTM_and_Progress_v1.1.xlsx`, `IAMS_RTM_Report_v1.1.md` |

Documents 1–7 carry a documented, independently-reviewed history (93 tracked review issues, 84 resolved, 8 partially resolved as deployment/backlog residuals, 0 open as of this baseline — see the RTM Report). Documents 8–10 were reconciled to this baseline on 2026-07-10 from pre-existing "Draft for Engineering Implementation" specifications; their reconciliation changed only version citations and requirement-ID references, not design substance (see each document's own changelog note).

## 2. What You're Actually Being Asked to Confirm

By signing, you are confirming — for your row only, not the whole set — that you have read your assigned document(s), that they correctly represent your area's requirements, and that you accept them as binding for R1 development. You are **not** confirming that the whole system is bug-free, that R2/R3 scope is final, or that no future change requests will occur (BRD §6.3 governs how changes flow through this baseline afterward).

| Role | Person | Assigned Document(s) | Confirms | Signature | Date |
|---|---|---|---|---|---|
| Executive Sponsor | _________________ | BRD 2.0 §1 (Objectives), §8 (Release Plan), §11 (Constraints) | Business objectives, success measures, and R1/R2/R3 scope split are correct and fundable | _________________ | _______ |
| Inventory Manager (business owner) | _________________ | BRD 2.0 §7 (BR-01, 06, 14), FRS 2.0 §2.1/2.2/2.4 (AST/INV/LIF) | Asset, inventory, and lifecycle requirements match real operating practice | _________________ | _______ |
| IT Security Officer | _________________ | SRS 2.0 §4.4 (NFR-SEC), FRS 2.0 §2.12 (SEC), Middleware/Infra/Security Spec v1.1 | Security architecture, RBAC/SoD model, and break-glass/audit-log controls are acceptable; **pen-test go-live gate scheduled** (see companion Pen-Test Engagement Checklist) | _________________ | _______ |
| Data Protection / Compliance Officer | _________________ | BRD 2.0 §9 (Compliance posture), FRS 2.0 §2.15 (CMP) | Named regulatory postures (GDPR / India DPDP / FERPA-aligned) and retention/legal-hold/anonymization mechanics are sufficient for go-live sign-off obligations (BRD §6.5) | _________________ | _______ |
| IT / Infrastructure Team Lead | _________________ | SRS 2.0 §2 (Architecture), §3 (Stack), §4.10 (Deployment), Backend Architecture Spec v1.1 | Deployment architecture (Docker Compose, MinIO, reverse proxy) is operable by the available team per BRD §11.2 | _________________ | _______ |
| Auditor / Audit Program Owner | _________________ | FRS 2.0 §2.5 (AUD), §2.6 (SCN), Acceptance Criteria §1/§7 | Audit workflow, offline-scan behavior, and evidence-immutability model reflect real field conditions | _________________ | _______ |
| Product Management | _________________ | All documents (methodology owner) | The baseline is internally consistent and this ratification record accurately lists every governing document | _________________ | _______ |

## 3. Known, Accepted Residuals at Time of Ratification

Signing does **not** require these to be zero — they are explicitly acknowledged as non-blocking for R1 Sprint 1, per the RTM Report v1.1 §3/§6:

- 8 "Partially Resolved" issues in the Issues Log — all deployment-configuration or backlog-level choices (e.g., variance-approval numeric thresholds, LDAP group→role mapping table, SMS provider selection), none requiring a requirements decision before coding starts.
- Third-party penetration test (FR-SEC-13) — a **go-live gate**, not a Sprint-1 gate. Must be scheduled with enough lead time to complete before first production deployment. See `IAMS_Penetration_Test_Engagement_Checklist_v1.0.md`.
- Migration cutover rehearsal (FR-MIG-05) — required before **production cutover**, not before Sprint 1. Runbook is ready; the rehearsal itself requires a staging environment and real data, which don't exist yet. See `IAMS_Migration_Cutover_Runbook_v1.0.md`.
- R2 Must-Have Acceptance-Criteria stubs (FRS 2.0 §4) — due before R2's first sprint, not R1's.

## 4. Effect of Signing

Once all rows above are signed, R1 Sprint 1 may begin against this baseline. Any subsequent change to a signed document follows BRD 2.0 §6.3–6.4 change governance (version increment, re-circulation to affected signers, no silent edits to ratified text).

**Ratification date (all signatures complete):** _________________
**Recorded by:** _________________
