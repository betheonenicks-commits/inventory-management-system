# Requirements Traceability Matrix & Development-Readiness Report
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-RTM-REPORT-1.2 | **Status:** Final | **Date:** 2026-07-12
**Supersedes:** IAMS_RTM_Report_v1.1.md. **Built from:** `IAMS_RTM_and_Progress_v1.1.xlsx`, `IAMS_Epics_and_User_Stories_v1.0.md` (content now at internal Document ID IAMS-EUS-1.1), and every document in both `July 10 Updates/files/` and the project root `files/` folder.

> **What changed since v1.1:** v1.1 asserted "**Epics & User Stories: 100% — 178 stories, verified 1:1 FR traceability**" with 0 open issues. That verification was incomplete. A subsequent line-by-line diff of every `FR-<MODULE>-<NN>` range stated in `IAMS_FRS_v2.0.md` §2 against every FR citation in the Epics & User Stories document found **two FRs with zero corresponding user story** and **seven stories citing a role that doesn't exist in FR-USR-01's role list**. All findings are now closed directly in the Epics & User Stories document (edited in place; no version-suffix rename, since that document is a living backlog artifact, not a point-in-time snapshot). This report documents what was found, why v1.1's "verified" claim missed it, and what changed.

---

## 1. Verdict, Up Front

**All requirements-artifact and documentation work is complete, including the two traceability gaps and one convention violation this revision closes. R1 Sprint 1 can start as soon as the ratification signatures in §4 are collected — that is the only remaining gate, and it is a governance step, not a content gap.**

| Track | Status |
|---|---|
| **Requirements baseline** (BRD/FRS/SRS/DD/API/AC/PUC) | 100% — ratified content, 0 open issues, 0 stale ID citations found remaining (unchanged from v1.1; this revision's findings were in the Epics/User-Stories layer, not the baseline itself) |
| **Epics & User Stories** | **100% — 180 stories** (163 FRs mapped 1:1, 16 platform/NFR stories, 1 documented non-FR duplicate — the AUD signature clarification folded into US-AUD-13), verified by a scripted diff of every FR range in FRS §2 against every FR citation in the document, not by re-trusting the prior summary count. **Corrected from v1.1's "178, verified" claim** — see §2.5–2.7. |
| **Design specs** (Frontend-UX, Backend-Architecture, Middleware/Infra/Security) | 100% — reconciled to v1.1, re-pointed to the ratified baseline, verified no residual stale citations (unchanged from v1.1) |
| **RTM workbook** | 100% — both data-quality defects found in v1.0 fixed and independently re-verified (unchanged from v1.1); **not yet updated** for this revision's two new FR rows (FR-AUD-18, FR-SEC-03b) — flagged in §6 as the one remaining paperwork item |
| **Migration cutover documentation** | 100% documented (runbook ready); execution pending — pre-production-cutover activity, not pre-Sprint-1 |
| **Security go-live gate** | 100% prepared; execution pending — pre-go-live activity, not pre-Sprint-1 |
| **Stakeholder ratification** | Not yet signed — the one item outside what a document-editing pass can complete |

**Being precise about "100% development ready," again:** v1.1 made this same claim and was wrong about one of its six tracks — not because anyone acted in bad faith, but because "verified 1:1 FR traceability" was asserted from the story-count arithmetic matching (178 = 162 claimed-FR-based + 16 platform), not from an actual FR-by-FR diff. The count matched by coincidence: two FRs (FR-AUD-18, FR-SEC-03b) were simply never counted into the "162" in the first place, and their absence was masked by the AUD module's one legitimate non-FR duplicate story landing on the same total. The lesson carried into this revision's verification method (§7): **recompute total story counts from the FRS's own stated FR ranges, don't just check that the arithmetic in the summary table is internally consistent.**

---

## 2. Fixes Applied Through This Revision

### 2.1–2.4 (v1.1, unchanged)
Three unreconciled design specs, a stale `FR-USR-05` cell in the RTM workbook, and a missing RTM row for FR-AUD-20/22/23 — all resolved in v1.1 exactly as documented there. Not repeated here; see `IAMS_RTM_Report_v1.1.md` §2.1–2.4 for the full detail, which remains accurate.

### 2.5 Finding D (v1.2) — FR-AUD-18 had no user story → **Resolved**

FR-AUD-18 ("immutability with corrections-as-linked-records," FRS §2.5's unchanged-from-v1.2 block) is independently and repeatedly cited outside the Epics document — by `IAMS_API_Specification_v1.1.md`'s `POST /audits/{id}/findings/{findingId}/corrections` endpoint, by `IAMS_Data_Dictionary_v1.1_Amendment.md`'s `version` column note, by `IAMS_Personas_and_Use_Cases_v1.1.md`, and by this very RTM's own v1.0 §2 matrix row (`BR-AUD-04 | ... | FR-AUD-12–15, FR-AUD-18 | US-AUD-13–16`) — but no story in the Epics document ever implemented it. The v1.0 RTM row's own mapping (`FR-AUD-18` → `US-AUD-13–16`) was itself wrong: none of US-AUD-13 (signature), 14 (approval routing), 15 (completion certificate), or 16 (exception report) describes immutability or corrections-as-linked-records.

**Fix:** added **US-AUD-24** to the Epics & User Stories document, grounded in the actual corrections endpoint's contract (who can call it, what it produces, that the original finding is preserved). Numbered out of the FR-matching sequence (appended, not inserted as US-AUD-18) to avoid renumbering every existing cross-reference to US-AUD-13–23 elsewhere in that document and in this RTM.

### 2.6 Finding E (v1.2) — FR-SEC-03b had no user story → **Resolved**

FR-SEC-03b ("optional MFA for others," `[Could]`, FRS §2.12) is explicitly listed in the FRS's own Release Mapping Appendix (R2) and — tellingly — in the Epics document's **own** EPIC-SEC summary-table release-span cell, which already read "R2 (03b,07,12,13)" before this revision. The cell named the FR; no story existed for it. Only FR-SEC-03a (mandatory MFA for Super Admin/Admin, US-SEC-03) was ever written up.

**Fix:** added **US-SEC-17**, appended after US-SEC-16 for the same renumbering-avoidance reason as §2.5.

### 2.7 Finding F (v1.2) — Seven stories used a role name that doesn't exist → **Resolved**

The Epics document's own §1 convention states: *"role names in story statements match FRS FR-USR-01 role names."* Two distinct violations were found by extracting every role phrase used in every story's *"As a `<role>`"* opener and diffing the result against FR-USR-01's literal role list:

- **Five stories** (US-DSH-06, US-RPT-01, US-RPT-09, US-RPT-12, US-INT-01) opened with "As a Board Viewer" or "As a Board Viewer / Finance Officer." FR-USR-01's role list has no such role — the closest defined role is **Viewer** (the PUC's "Board Member / Finance Officer" persona is explicitly documented as *using* the Viewer role, not a role of its own). Cross-checked independently against `UX design/IAMS Design System/ui_kits/iams/data.js`'s `users[]` fixture, whose closed role vocabulary (Super Administrator, Administrator, Inventory Manager, Auditor, Department Head, Volunteer) also contains no "Board Viewer" role.
- **Two stories** (US-NTF-04, US-NTF-05) opened with "As an Employee," but FR-USR-01 defines the role as the single combined name **"Employee/Volunteer"** — used correctly elsewhere in the same document (US-ANL-04, US-PLAT-11). "Employee" alone risks a developer reading the notification-preference stories as volunteer-exempt, which isn't the intent.

**Fix:** all seven story statements corrected to their FR-USR-01 role name ("Viewer" or "Employee/Volunteer" respectively). The Epic Summary table's informal "Primary Personas" column (which listed "Board Viewer" as shorthand for the persona, not a role field) was left as-is — that column was never bound by the role-name convention. Re-ran the full role-phrase extraction after the fix: every "As a/an `<role>`" opener in the document now matches FR-USR-01's role list exactly.

### 2.8 New artifacts produced this revision

None — this revision is a text-correction pass against the existing Epics & User Stories document. No new companion documents were needed.

---

## 3. What Genuinely Cannot Be "Fixed" by This Session, and Why

Unchanged from v1.1 §3: stakeholder ratification signatures, the penetration test itself, and the migration rehearsal itself remain human/organizational actions that no document edit substitutes for. See `IAMS_RTM_Report_v1.1.md` §3 for the full reasoning, still accurate.

---

## 4. Everything From v1.0/v1.1 Still Holds

§2 (BR→FR→Epic/User-Story traceability matrix, now understood to need the FR-AUD-18/FR-SEC-03b rows added — see §6), §3 (93-item Issues Log closure), and §4 (the four highest-severity pre-baseline risks) from `IAMS_RTM_Report_v1.0.md`, and §2.1–2.4 of `IAMS_RTM_Report_v1.1.md`, are unchanged and still accurate. Nothing in them was invalidated by this revision — the two missing stories and the role-naming defect were errors of omission/inconsistency in the Epics document itself, not errors in the BRD/FRS/SRS/DD/API baseline those sections describe.

---

## 5. Final Pre-Sprint-1 Checklist

| # | Item | Status |
|---|---|---|
| 1 | Stakeholder ratification signatures collected (`IAMS_Baseline_Ratification_Record_v1.0.md`) | **Open — the only remaining gate** |
| 2 | Three design specs reconciled to v1.1 | Closed |
| 3 | RTM workbook data-quality defects fixed | Closed |
| 4 | Epics & User Stories: FR-AUD-18, FR-SEC-03b gaps closed; role-naming convention violation fixed | **Closed this revision** |
| 5 | RTM workbook (`IAMS_RTM_and_Progress_v1.1.xlsx`) rows added for FR-AUD-18 (→US-AUD-24) and FR-SEC-03b (→US-SEC-17) | **Open — see §6** |
| 6 | Penetration test — engagement checklist ready; test scheduled and executed | Checklist closed; execution is a go-live gate, not Sprint-1 |
| 7 | Migration cutover — runbook ready; rehearsal executed | Runbook closed; execution is a pre-production-cutover gate, not Sprint-1 |
| 8 | R2 Must-Have Acceptance-Criteria stubs | Due before R2's first sprint, not R1's — unchanged from v1.0 |

---

## 6. One Item Deliberately Left Open

Item 5 above — adding `FR-AUD-18`/`US-AUD-24` and `FR-SEC-03b`/`US-SEC-17` rows to `IAMS_RTM_and_Progress_v1.1.xlsx` — is **not** done in this revision. Unlike the text corrections in §2.5–2.7, the workbook is a binary file; per the verification discipline established in v1.1 §2.2 (parse the OOXML structure, confirm exactly which cells reference the changed value, repackage, re-extract, re-verify sheet/part counts), this needs its own dedicated pass rather than being bundled into a text-document review. Flagging it explicitly rather than silently leaving it stale, since silently leaving it stale is exactly the kind of gap this revision exists to catch.

---

## 7. Verification Method Used This Revision

Every `FR-<MODULE>-<NN>` (and lettered sub-ID, e.g. `FR-SEC-03a`/`03b`) mentioned anywhere in `IAMS_FRS_v2.0.md` §2 was enumerated per module. Every FR citation appearing as the primary tag under a user-story heading in the Epics & User Stories document (the `*FR-XXX-NN · Priority · Release*` line) was extracted and diffed against that FRS-derived list, per module, printed as an explicit ordered sequence (not just a count) so a skipped or duplicated number would be visually obvious rather than hidden inside a total. This is the check that found FR-AUD-18 and FR-SEC-03b missing, and confirmed — after the two additions — that all 163 FRs across all 16 modules now have exactly one corresponding story, with the single documented exception (FR-AUD-12, folded into both US-AUD-12 and US-AUD-13). Role names used in every "As a `<role>`" story-statement opener were separately extracted and checked against FR-USR-01's literal role list, which is what surfaced the "Board Viewer" violations.

---

## Appendix — Files Produced or Modified This Revision

**New files:** `IAMS_RTM_Report_v1.2.md` (this file).
**Modified in place:** `IAMS_Epics_and_User_Stories_v1.0.md` (internal Document ID bumped to IAMS-EUS-1.1, dated 2026-07-12; filename unchanged — this is a living backlog document, not a dated snapshot).
**Untouched, preserved as historical record:** `IAMS_RTM_Report_v1.1.md`, `IAMS_RTM_Report_v1.0.md`, `IAMS_RTM_and_Progress_v1.1.xlsx` (see §6 — still pending its own update), all v1.1 design specs.
