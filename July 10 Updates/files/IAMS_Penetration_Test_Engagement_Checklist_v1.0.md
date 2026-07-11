# Penetration Test Engagement Checklist
## Inventory Audit Management System (IAMS)

**Document ID:** IAMS-PENTEST-CHECKLIST-1.0 | **Status:** Ready for Use | **Date:** 2026-07-10
**Fulfills:** FR-SEC-13 ("a third-party penetration test is a go-live gate for the first production deployment"). **This document prepares the engagement — it is not the test itself, and no test has been performed.** A penetration test requires an actual running system, an actual engaged third-party firm, and actual time on both sides; none of that can be produced by editing documents. What this checklist does is remove every reason the engagement would stall once R1 is feature-complete enough to test.

---

## 1. When to Trigger This

Per FR-SEC-13 and the RTM Report v1.1 gate checklist, this is a **go-live gate**, not a Sprint-1 gate — but engaging a third-party firm has lead time (typically 4–8 weeks from contract to test window for a competent firm), so **initiate vendor selection during R1 development, not after R1 is "done."** Target: test window scheduled to complete with enough buffer before the planned first production go-live date to allow remediation of any Critical/High findings and a re-test if needed.

## 2. Scope Definition (fill in before requesting quotes)

| Item | Answer |
|---|---|
| Target environment | Staging, production-equivalent snapshot (per SRS 2.0 §2.6) — **never test against a live customer's production data** |
| In-scope surface | REST API (`/api/v1`), React SPA/PWA, authentication (local + LDAP/AD + SSO paths if configured for the pilot deployment), file upload/object-store brokering path, webhook signing (if INT module enabled for the test), rate-limiting/API-gateway layer |
| Out-of-scope | The organization's own network infrastructure outside the Docker Compose stack (unless explicitly contracted as a broader infra assessment) |
| Test type | Authenticated + unauthenticated web application penetration test, plus API-specific testing (auth bypass, IDOR/org-scope-escape, injection, business-logic abuse of the audit/approval workflows) |
| Explicitly requested focus areas | (1) Org-scope enforcement — can a low-privilege role reach data outside its scope via direct object reference (AC-ORG-05-X's threat model); (2) Separation-of-duties bypass — can a submitter self-approve without a valid waiver (AC-USR-06-X); (3) File-upload path — does the content-type/size validation (NFR-SEC-10) actually block a malicious payload before it reaches the object store; (4) Break-glass abuse — can the time-boxed elevation (FR-SEC-16) be invoked without triggering the mandatory dual notification; (5) Webhook SSRF — does the allow-list (NFR-SEC-11) actually reject an unregistered callback URL |
| Excluded by design | Denial-of-service / availability testing (coordinate separately if desired — DoS testing against a single-node on-prem deployment risks an actual outage, not just a finding) |

## 3. Pre-Engagement Prerequisites (must be true before the test window opens)

- [ ] Staging environment deployed and stable, seeded with realistic (not production-real) test data across all R1 modules
- [ ] At least one account per role (Super Admin, Administrator, System Operator, Inventory Manager, Auditor, Read-only Auditor, Department Head, Employee/Volunteer, Viewer) provisioned for the testers, with credentials handed over through a secure channel (not email in cleartext)
- [ ] `IAMS-AC-1.0` (Acceptance Criteria) and `IAMS_API_Specification_v1.1.md` shared with the testing firm — a tester working from the actual authorization contract finds business-logic flaws faster than one working blind
- [ ] Rate limiting (NFR-API-01) either temporarily relaxed for the test IP range or the testing firm briefed on the limits, so findings aren't just "your rate limiter works"
- [ ] A named internal point of contact available during the test window for real-time triage of anything that looks like it might affect a shared/adjacent system

## 4. Vendor Selection Criteria

- [ ] Demonstrated experience testing Spring Boot / REST API backends, not just generic web-app scanning
- [ ] Willing to test business-logic/authorization flaws specific to a role-based, org-scoped system, not just OWASP Top 10 automated-scan output
- [ ] Provides a written report with CVSS-scored findings and reproduction steps, not a slide deck
- [ ] Offers a re-test window for remediated Critical/High findings, scoped into the original contract (avoid a second procurement cycle for re-test)

## 5. Go-Live Decision Rule

| Finding severity | Effect on go-live |
|---|---|
| Critical | **Blocks go-live** until remediated and re-tested |
| High | **Blocks go-live** until remediated and re-tested, unless the IT Security Officer records an explicit, time-boxed risk acceptance (mirrors the SoD-waiver pattern already used elsewhere in this system — a documented exception, never a silent one) |
| Medium | Does not block go-live; must be triaged into the post-launch backlog with an owner and target date before go-live sign-off |
| Low / Informational | Logged; no gating action required |

## 6. Linkage to the Vulnerability Disclosure/Patching Policy

FR-SEC-13 also requires "a documented vulnerability patching and disclosure policy for on-premises customers" as a standing artifact, separate from the one-time pre-launch test. At minimum, that policy must define: how a customer or external researcher reports a suspected vulnerability, target response-time SLAs by severity, and how patches are distributed to on-premises deployments that have no mandatory outbound connectivity (BRD §5.2) — i.e., a pull-based update mechanism (NFR-DEPLOY-04's versioned-release/image-tag-upgrade path), not an auto-push. **This policy still needs to be authored** — it is out of scope for this checklist, which only covers the pre-launch test engagement, and should be tracked as its own deliverable owned by the IT Security Officer.

## 7. Status

| Step | Status |
|---|---|
| Scope defined (§2) | Drafted in this document — needs IT Security Officer review before it's final |
| Vendor selected | **Not started** |
| Test window scheduled | **Not started** |
| Test executed | **Not started** |
| Findings remediated | **Not started** |
| Re-test (if needed) | **Not started** |
| Go-live sign-off | **Blocked on all of the above** |
