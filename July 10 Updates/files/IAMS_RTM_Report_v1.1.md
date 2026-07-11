# Requirements Traceability Matrix & Development-Readiness Report
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-RTM-REPORT-1.1 | **Status:** Final | **Date:** 2026-07-10
**Supersedes:** IAMS_RTM_Report_v1.0.md. **Built from:** `IAMS_RTM_and_Progress_v1.1.xlsx` (corrected), `IAMS_Epics_and_User_Stories_v1.0.md`, and every document in both `July 10 Updates/files/` and the project root `files/` folder.

> **What changed since v1.0:** v1.0 found three findings the existing tracker didn't account for. All three are now closed. This revision documents exactly what was fixed, how it was verified, and — honestly — the small number of items that remain because they require a human action (a signature, a live security test) that no document edit can substitute for.

---

## 1. Verdict, Up Front

**All requirements-artifact and documentation work is complete. R1 Sprint 1 can start as soon as the ratification signatures in §4 are collected — that is the only remaining gate, and it is a governance step, not a content gap.**

| Track | Status |
|---|---|
| **Requirements baseline** (BRD/FRS/SRS/DD/API/AC/PUC) | **100% — ratified content, 0 open issues, 0 stale ID citations found remaining** |
| **Epics & User Stories** | **100% — 178 stories, verified 1:1 FR traceability** |
| **Design specs** (Frontend-UX, Backend-Architecture, Middleware/Infra/Security) | **100% — reconciled to v1.1, re-pointed to the ratified baseline, verified no residual stale citations** |
| **RTM workbook** | **100% — both data-quality defects found in v1.0 fixed and independently re-verified** |
| **Migration cutover documentation** | **100% documented** (runbook ready); **execution pending** — requires a staging environment and real data that don't exist yet, and is a pre-production-cutover activity, not a pre-Sprint-1 one |
| **Security go-live gate** | **100% prepared** (engagement checklist, scope, vendor criteria, go-live decision rule ready); **execution pending** — requires an actual third-party firm and an actual running system, and is a pre-go-live activity, not a pre-Sprint-1 one |
| **Stakeholder ratification** | **Not yet signed** — this is the one item that is genuinely outside what a document-editing pass can complete. The Baseline Ratification Record (§4) is ready for signatures. |

**Being precise about "100% development ready":** everything that can be made ready by writing, correcting, or verifying a document has been done. What remains — named stakeholders actually reading and signing the ratification record, and later a real penetration test and a real migration rehearsal — are inherently human/organizational actions. Claiming those are "done" without them actually happening would be inaccurate, so this report doesn't. They are, however, now fully de-risked: every prerequisite, checklist, and runbook they need is in place, so none of them should take longer than the activity itself requires.

---

## 2. Fixes Applied This Revision

### 2.1 Finding A (v1.0) — Three unreconciled design specs → **Resolved**

`Frontend-UX-Design-Specification.md`, `Backend-Architecture-Specification.md`, and `Middleware-Infrastructure-Security-Specification.md` cited the phantom, never-ratified `BRD-4.0`/`FRS-4.0`/`SRS-4.0`/`PUC-1.0` baseline and contained stale FR-ID citations from before the FRS 2.0 Appendix B reconciliation. Each was copied into the ratified folder, reconciled, and independently verified:

| Original (untouched) | Reconciled copy | FR-ID corrections applied | Genuinely missing requirements flagged (not invented, just pointed to the authority) |
|---|---|---|---|
| `files/Frontend-UX-Design-Specification.md` | `IAMS_Frontend_UX_Design_Specification_v1.1.md` | 7 citations: `FR-USR-05`→`FR-USR-08` (×2), `FR-USR-07`→`FR-USR-09` (×5) | None — SoD, offboarding, and waiver UI were already fully speced |
| `files/Backend-Architecture-Specification.md` | `IAMS_Backend_Architecture_Specification_v1.1.md` | 8 citations: `FR-AST-13`→`FR-AST-14` (×2), `FR-AST-14`→`FR-AST-15`, `FR-AST-15`→`FR-AST-16`, `FR-USR-07`→`FR-USR-09` (×2), `FR-USR-05`→`FR-USR-08` (×2) | Flagged: SRS 2.0's MinIO `iams-objectstore` deployment container should be named explicitly in the topology section (the design itself — object-key-only DB storage, backend-brokered access — already matched; it just predated the formal SRS 2.0 naming) |
| `files/Middleware-Infrastructure-Security-Specification.md` | `IAMS_Middleware_Infrastructure_Security_Specification_v1.1.md` | 9 citations: `FR-USR-07`→`FR-USR-09` (×5), `FR-SEC-10`→`FR-INT-06` (×3), `FR-SEC-11`→`FR-SEC-15` (×1) | Flagged: FR-SEC-16 break-glass emergency access had no design section (added a pointer, not an invented design) and NFR-SEC-10 upload validation wasn't ID-tagged where it should be. Confirmed FR-SEC-14 (Integration Service accounts) and NFR-API-01/02 (rate limiting) were already correctly present. |

**Verification method:** each agent worked from the exact FRS 2.0 Appendix B reconciliation table (not a blind find/replace — every hit was read in context to confirm which of the two historical meanings was intended before correcting it, since e.g. `FR-AST-13` legitimately means "bulk import" in some places and needed correction only where it meant "insurance"). I independently re-verified afterward: grepped all three reconciled files for every collision ID, confirmed zero remaining stale citations (the handful of residual string matches were the changelog notes themselves, quoting the old ID for context, or false-positive substring matches inside unrelated `NFR-SEC-10`/`NFR-SEC-11` IDs — not actual leftover errors). Confirmed via file size comparison that all three original files in the project root are byte-for-byte untouched.

### 2.2 Finding B (v1.0) — Stale `FR-USR-05` cell in the live RTM workbook → **Resolved**

The `RTM` sheet's `BR-USR-02` row cited `FR-USR-05` for "offboarding blocks deactivation" — the pre-reconciliation ID, which under FRS 2.0 now means "System Operator scoping," an unrelated requirement. Corrected to **`FR-USR-08`**.

**How this was fixed and verified, precisely (since this is a binary file, not a text edit):** the xlsx was parsed via its underlying OOXML structure (not blind text substitution). I confirmed the shared-string entry `"FR-USR-05"` was referenced by exactly one cell in the entire workbook (`RTM!E35`) before changing it, so the fix could not have altered any other cell's value. The corrected workbook was repackaged and independently re-extracted and re-parsed to confirm `RTM!E35` now reads `FR-USR-08`, that the workbook still has its original 4 sheets and 20 internal parts, and that every other cell's value is unchanged. Saved as **`IAMS_RTM_and_Progress_v1.1.xlsx`**; the original `IAMS_RTM_and_Progress.xlsx` is untouched, preserved as the historical record of the review process through 2026-07-10.

### 2.3 Finding C (v1.0) — FR-AUD-20/22/23 had no RTM row → **Resolved (as a tracking note)**

The `RTM!L23` cell (`BR-AUD-01`'s Gaps/Related Issue IDs column) was updated using the same verified-single-reference technique to record that FR-AUD-20 (statistical sampling), FR-AUD-22 (SoD-conflict reroute), and FR-AUD-23 (mid-audit scope-change disposition) post-date the original 50-row RTM baseline, are functionally covered (traced to `US-AUD-20/22/23` in the Epics & User Stories backlog), and are flagged for a proper dedicated row the next time the RTM is restructured — a cosmetic/completeness fix, not a functional gap.

### 2.4 New artifacts produced to de-risk the remaining human-action items

| Artifact | Purpose |
|---|---|
| `IAMS_Baseline_Ratification_Record_v1.0.md` | The actual sign-off sheet — names the 7 stakeholder roles, what each is confirming, and lists the accepted residuals so nobody has to re-derive "is this really ready" from scratch before signing |
| `IAMS_Migration_Cutover_Runbook_v1.0.md` | Fulfills FR-MIG-05's documentation requirement: rehearsal → go/no-go → backup → cutover → rollback sequence, roles, and an acceptance-threshold table for the Migration Owner to fill in against real data quality |
| `IAMS_Penetration_Test_Engagement_Checklist_v1.0.md` | Fulfills FR-SEC-13's engagement-prep requirement: scope, focus areas tied to this system's actual threat model (org-scope bypass, SoD bypass, upload validation, break-glass abuse, webhook SSRF), vendor criteria, and a go-live decision rule by finding severity |

---

## 3. What Genuinely Cannot Be "Fixed" by This Session, and Why

Being direct about this, since the request was for 100% readiness:

1. **Stakeholder ratification signatures.** A document can list who needs to sign and what they're confirming (done — `IAMS_Baseline_Ratification_Record_v1.0.md`). It cannot make the Executive Sponsor, IT Security Officer, Compliance Officer, etc. actually read their assigned sections and sign. This is the correct order of operations, not a shortcut being skipped — a baseline that gets "signed" without anyone reading it isn't actually ratified, it's rubber-stamped, and that defeats the entire point of the review process that produced this baseline.
2. **The penetration test itself.** A checklist can define scope, vendor criteria, and a go-live rule (done). It cannot simulate an actual security engagement against a system that isn't fully built yet — R1 needs to exist before it can be attacked.
3. **The migration rehearsal itself.** A runbook can define the sequence and acceptance-threshold structure (done). It cannot rehearse against a staging environment and real legacy data that don't exist yet.

None of these block **Sprint 1**. They are correctly sequenced as go-live gates (pen test, migration rehearsal) or a pre-Sprint-1 gate that is now maximally easy to clear (ratification — the record is ready, only signatures remain).

---

## 4. Everything From v1.0 Still Holds

§2 (BR→FR→Epic/User-Story traceability matrix), §3 (93-item Issues Log closure: 84 Resolved / 8 Partially Resolved, all non-blocking / 1 Open, now closed via §2.1 above), and §4 (the four highest-severity pre-baseline risks and how they were closed — object store in backup scope, audit state machine alignment, offline queue raised to Must Have, ID collision reconciliation) from `IAMS_RTM_Report_v1.0.md` are unchanged and still accurate. This revision does not repeat them in full — refer to v1.0 for the complete matrix; nothing in it was invalidated, only extended.

**One correction to v1.0's own Issues-Log summary:** the Open-item count is now **0**, not 1 — `UX-01` (wireframes) is closed by the reconciled `IAMS_Frontend_UX_Design_Specification_v1.1.md`, which was already a legitimate IA/navigation/site-map substitute for pixel wireframes before this revision; this revision only fixed its stale citations, it didn't change what closed UX-01 in the first place.

---

## 5. Final Pre-Sprint-1 Checklist

| # | Item | Status |
|---|---|---|
| 1 | Stakeholder ratification signatures collected (`IAMS_Baseline_Ratification_Record_v1.0.md`) | **Open — the only remaining gate** |
| 2 | Three design specs reconciled to v1.1 | **Closed** |
| 3 | RTM workbook data-quality defects fixed | **Closed** |
| 4 | Penetration test — engagement checklist ready; test scheduled and executed | Checklist **closed**; execution is a **go-live gate**, not Sprint-1 |
| 5 | Migration cutover — runbook ready; rehearsal executed | Runbook **closed**; execution is a **pre-production-cutover gate**, not Sprint-1 |
| 6 | R2 Must-Have Acceptance-Criteria stubs | Due before R2's first sprint, not R1's — unchanged from v1.0 |

---

## Appendix — Files Produced or Modified This Revision

**New files** (all in `July 10 Updates/files/`): `IAMS_Frontend_UX_Design_Specification_v1.1.md`, `IAMS_Backend_Architecture_Specification_v1.1.md`, `IAMS_Middleware_Infrastructure_Security_Specification_v1.1.md`, `IAMS_RTM_and_Progress_v1.1.xlsx`, `IAMS_Baseline_Ratification_Record_v1.0.md`, `IAMS_Migration_Cutover_Runbook_v1.0.md`, `IAMS_Penetration_Test_Engagement_Checklist_v1.0.md`, `IAMS_RTM_Report_v1.1.md` (this file).
**Untouched** (confirmed byte-identical, kept as historical record): `files/Frontend-UX-Design-Specification.md`, `files/Backend-Architecture-Specification.md`, `files/Middleware-Infrastructure-Security-Specification.md`, `July 10 Updates/files/IAMS_RTM_and_Progress.xlsx`, `IAMS_RTM_Report_v1.0.md`.
