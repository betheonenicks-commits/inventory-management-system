**Middleware, Security & Infrastructure Specification**

**Inventory Audit Management System (IAMS)**

*Filter Chains, AuthN/AuthZ, Secrets, Rate Limiting, Deployment Topology, Integration Gateway, CI/CD, Backup/DR, Observability, Incident Response*

Document ID: IAMS-MIS-1.1 | Version: 1.1 | Status: Reconciled to Baseline v2.0 — Ready for Engineering | Date: 2026-07-10

Related Documents: IAMS-BRD-2.0 (Business Requirements), IAMS-FRS-2.0 (Functional Requirements), IAMS-SRS-2.0 (Architecture, NFRs, Security Architecture), IAMS-PUC-1.1 (Personas & Use Cases)

# Document Control

## Purpose

The SRS (IAMS-SRS-2.0, Section 6) states *what* the security architecture must guarantee. This document is the implementation-ready specification of *how* that architecture is built and operated: the concrete, ordered Spring Security filter chain; JWT/LDAP/SSO mechanics; the composition of RBAC, org-scope, and Separation-of-Duties (SoD) enforcement; secrets management; rate limiting; the Docker Compose service topology; the Integration Gateway; the CI/CD pipeline; the scripted backup/restore tool; observability instrumentation; and the incident response runbook. It is written for the backend/DevOps engineers who build and operate IAMS, not for business or product stakeholders.

Every design decision below either implements a control already fixed by the SRS/FRS/BRD (cited by ID) or resolves an ambiguity those documents deliberately left open — in the latter case, the decision is stated explicitly with its rationale, not left implicit. A consolidated log of these judgment calls is in Appendix B for later re-review.

## Governing Constraint

BRD Section 11.2 states plainly: *"Budget and IT staffing at many target organizations is limited, so operational complexity (backup, upgrades, security patching) must be kept low."* Every choice in this document is weighed against that constraint first. Where a more sophisticated option exists (Vault, Kubernetes, a paid SaaS CI vendor, Redis-backed distributed rate limiting), it is offered as an optional upgrade path, never as the mandatory default — consistent with the target persona: Priya (Super Administrator, IAMS-PUC-1.1), a part-time or overloaded IT generalist, not a dedicated platform team.

## Revision History

| Version | Date | Author | Description |
|---|---|---|---|
| 1.0 | 2026-07-09 | Engineering / DevOps | Initial implementation-ready middleware, security, and infrastructure specification |
| 1.1 | 2026-07-10 | Engineering / DevOps | Reconciled to ratified baseline v2.0 (BRD/FRS/SRS 2.0, PUC 1.1); corrected stale FR-ID citations; cross-checked against new SRS 2.0 requirement families |

---

> **v1.1 (2026-07-10):** references re-pointed to the consolidated baseline (BRD 2.0 / FRS 2.0 / SRS 2.0 / PUC 1.1); corrected stale FR-SEC-10/FR-SEC-11 citations per FRS 2.0 Appendix B (FR-SEC-10 → FR-INT-06 for webhook signing, FR-SEC-11 → FR-SEC-15 for credential storage) and FR-USR-07 → FR-USR-09 (SoD waivers); cross-checked against new SRS 2.0 NFR-CONC/OBS/API families and FR-SEC-14/16 — flagged two genuinely missing items (FR-SEC-16 break-glass access, NFR-SEC-10 upload validation) with pointers to the authoritative requirement; confirmed FR-SEC-14 (Integration Service accounts) and NFR-API-01/02 (rate limiting) were already correctly implemented. Content otherwise unchanged.

---

# 1. Spring Security Filter/Middleware Chain

## 1.1 Two-Layer Model: Reverse Proxy vs. Application

IAMS enforces security controls at two layers, and the split is deliberate, not incidental:

- **Reverse proxy (Nginx/Traefik, `iams-reverse-proxy`)** enforces everything that can be decided **without knowing who the user is** — TLS, coarse network-level abuse protection, and routing. This is the cheapest place to reject bad traffic: it costs no JVM thread, no Spring Security filter evaluation, no database connection. It is also the mandatory TLS termination point per SRS 6.3, and the only layer besides the frontend that is reachable from outside the Docker network (Decision #2 / SRS 2.5).
- **Application (Spring Boot, `iams-backend`)** enforces everything that requires **parsed identity, business state, or domain data** — authentication, RBAC, org-scope, the SoD domain-check, step-up re-authentication, and per-user/per-API-key rate accounting. These checks need JWT claims, database lookups, or entity state the proxy cannot cheaply evaluate without adding non-stock modules (e.g., Nginx Lua/njs), which would itself violate the low-operational-complexity constraint (BRD 11.2).

The rule of thumb: **if the decision only needs the request, it belongs at the proxy; if it needs the user, it belongs in the application.**

## 1.2 Reverse Proxy Stage (outside the JVM)

| # | Stage | Purpose | Requirement |
|---|---|---|---|
| R1 | TLS termination (TLS 1.2 minimum, 1.3 preferred) | Mandatory encryption boundary; backend is never reachable unencrypted from outside the Compose network | NFR-SEC-03, SRS 6.3 |
| R2 | Security header injection (HSTS, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy`, `Referrer-Policy: strict-origin-when-cross-origin`) | Browser-side defense in depth | SRS 6.3, OWASP ASVS V14 |
| R3 | Connection/body-size limits (`client_max_body_size`, `client_body_timeout`) | Coarse DoS protection before a request reaches the JVM | NFR-API-01 (defense in depth) |
| R4 | Correlation ID assignment (`$request_id` if `X-Correlation-Id` absent, else pass through) | End-to-end request tracing origin point | NFR-OBS-03 |
| R5 | Coarse per-IP rate limiting (`limit_req_zone`) | Pre-authentication flood/credential-stuffing protection — the proxy doesn't know *who* is calling yet, only *how often* from where | NFR-API-01 |
| R6 | Static frontend asset serving (large-deployment topology only) | Serves the React SPA bundle directly; small deployments instead let `iams-backend` serve it (Section 7) | SRS 2.5 |
| R7 | Reverse-proxy routing to `iams-backend` upstream | Only `iams-reverse-proxy` and (optionally) `iams-frontend` are network-exposed; `iams-db` and `iams-object-store` are never reachable from outside `iams-internal` | Decision #2, SRS 6.8 |
| R8 | Optional IP allow/deny enforcement | Cheapest rejection point for an org that restricts access to known office/VPN ranges | FR-SEC-07 |

## 1.3 Application Filter Chain (inside the JVM)

The chain below is a single `SecurityFilterChain` bean (`SessionCreationPolicy.STATELESS` — no server-side HTTP session; JWT bearer auth only) with custom filters inserted via `HttpSecurity.addFilterBefore/After` relative to Spring Security's default filters. Order matters and is enforced by explicit filter registration, not implicit bean ordering.

| # | Filter | Type | What it does | Requirement |
|---|---|---|---|---|
| 1 | `CorrelationIdFilter` | Custom `OncePerRequestFilter`, registered before every other filter | Trusts `X-Correlation-Id` from the reverse proxy (the only network path in); generates a UUID if absent (e.g., direct container-to-container calls in tests); writes it to the SLF4J MDC key `correlationId` and to the response header | NFR-OBS-03 |
| 2 | `CorsFilter` | Spring Security built-in, backed by a `CorsConfigurationSource` bean | Resolves CORS preflight before authentication runs (Section 4) | Section 4 |
| 3 | `RequestPayloadValidationFilter` | Custom `OncePerRequestFilter` | Content-Type allow-list, request-size guard (defense in depth behind R3), rejects malformed bodies before they reach Jackson/Hibernate Validator | SRS 6.7 |
| 4 | `SecurityContextHolderFilter` | Spring default | Stateless context holder — no session persistence, cleared per-request | NFR-SEC-01 |
| 5 | `JwtAuthenticationFilter` | Custom `OncePerRequestFilter`, positioned before `UsernamePasswordAuthenticationFilter` | Extracts `Authorization: Bearer`, validates signature/issuer/audience/expiry, checks the account's `securityStamp` claim against the current DB value (immediate revocation, Section 2.1), loads authorities + org-scope claims, populates `SecurityContext` | FR-SEC-01, NFR-SEC-01 |
| 6 | `ApiKeyAuthenticationFilter` | Custom `OncePerRequestFilter`, parallel to #5 | Authenticates integration/service-account calls via signed API credential (never a user JWT, never a blanket admin credential) | FR-SEC-09, FR-SEC-14 |
| 7 | `ExceptionTranslationFilter` | Spring default | Routes `AuthenticationException` → 401 JSON, `AccessDeniedException` → 403 JSON via custom entry points/handlers (never a redirect — this is an API, not a browser login flow) | NFR-MAINT-04 |
| 8 | `AuthorizationFilter` | Spring Security 6 default, URL-pattern rules | Coarse, fail-closed URL authorization (e.g., `/api/v1/admin/**` requires an admin-tier role) as a first line before fine-grained checks | NFR-SEC-02 |
| 9 | `OrgScopeResolutionFilter` | Custom `OncePerRequestFilter` | Resolves the org-hierarchy node(s) implicated by the request (path/query/body) into a request-scoped `OrgScopeContext` bean, so downstream policy evaluation (Section 3) doesn't re-derive it per check | FR-USR-04 |
| 10 | `StepUpRequirementInterceptor` | Spring MVC `HandlerInterceptor` (post-mapping, pre-invocation) | Inspects the resolved handler method for `@RequiresStepUp`; rejects with `STEP_UP_REQUIRED` if the caller's step-up assertion is missing/expired (Section 3.4) | NFR-SEC session policy, SRS 6.2 |
| 11 | `RateLimitAccountingFilter` | Custom `OncePerRequestFilter`, after authentication | Now that identity is known, enforces per-user and per-API-key Bucket4j token buckets (Section 6); short-circuits with 429 on violation | NFR-API-01, NFR-API-02 |
| 12 | `RequestResponseLoggingFilter` | Custom `OncePerRequestFilter` wrapping `chain.doFilter` | Emits one structured JSON log line per request after the response is committed (method, path, status, duration, userId, correlationId — Section 11) | NFR-OBS-03, NFR-MAINT-04 |
| — | **SoD domain-check** | *Not a filter* — a service-layer policy evaluation (Section 3.3) | Deliberately excluded from the generic filter chain because it requires loading the specific target entity (e.g., "did this user submit this audit?"), which isn't resolvable from the URL alone | FR-USR-06/09, FR-AUD-22 |
| 13 | `@ControllerAdvice GlobalExceptionHandler` | Spring MVC exception resolution (not a servlet filter, but the terminal stage of every request) | Translates domain exceptions (SoD violation, validation error, optimistic-lock conflict, not-found, integration failure) into a consistent JSON error envelope `{code, message, correlationId}`; never leaks stack traces or SQL | NFR-MAINT-04 |

**Why the SoD check sits below RBAC/org-scope, not inside the generic filter chain:** RBAC and org-scope answer "can this role, in this scope, ever do this kind of thing?" — a question answerable from the URL and the caller's claims alone, so it belongs in filters 8–9. SoD answers "can *this specific person* do this to *this specific record*, given who created it?" — a question that requires a repository lookup of domain state, which the generic filter chain has no business layer access to do without turning every filter into a mini-service. It is invoked explicitly inside the application/service layer (Section 3.3), guaranteeing it runs inside the same transaction as the action it's gating.

**Object-store upload validation (NFR-SEC-10) — flagged, not yet reflected above:** IAMS-SRS-2.0's NFR-SEC-10 requires that any file upload (an asset attachment/image ultimately bound for `iams-object-store`, Section 7) have its content-type and size validated **server-side, before any write reaches the object store** — a rejected file must never reach MinIO. Filter #3 (`RequestPayloadValidationFilter`, row above) already performs a generic Content-Type allow-list/size guard for request bodies broadly; this document's v1.0 baseline predates NFR-SEC-10 and does not yet specify whether upload-specific validation is folded into that same filter or implemented as a dedicated pre-write check in the asset-upload service path. Flagged here for engineering rather than left silently unaddressed; see IAMS-SRS-2.0, NFR-SEC-10 for the authoritative requirement.

---

# 2. Authentication (AuthN) Implementation

## 2.1 JWT Issuance, Validation, Refresh, Revocation

- **Signing algorithm:** RS256 (asymmetric). *Judgment call:* HS256 would be operationally simpler (one shared secret) for today's single-deployable modular monolith (SRS 2.1), but RS256 costs nothing extra to operate here and means the public verification key can be distributed to a future extracted service (or to the Integration Gateway for outbound-call verification) without ever exposing the private signing key — cheap insurance against SRS 2.1's stated intent to "extract services later if scale requires it." Key pair generated at first boot if absent, stored via the secrets mechanism (Section 5), rotatable via `kid` header (Section 5.4).
- **Access token:** 15-minute default TTL (configurable via `iams.security.jwt.access-ttl`), claims include `sub` (user id), `roles`, `orgScopes` (node ids the user's role assignments touch), `securityStamp` (see below), `authSource` (LOCAL/LDAP/SSO).
- **Refresh token:** opaque random 256-bit token (not a JWT), stored server-side **hashed** (SHA-256) in a `refresh_tokens` table (`user_id`, `token_hash`, `issued_at`, `expires_at`, `revoked_at`, `replaced_by_id`, `device_label`). Default TTL 12 hours, configurable up to 30 days for low-turnover volunteer accounts. **Rotation with reuse detection:** every refresh call issues a new refresh token and marks the old one `replaced_by_id`; if an already-replaced token is presented again (a stolen-and-replayed token racing the legitimate client), the entire token family is revoked and the account is flagged for a forced re-login — this satisfies NFR-SEC-01's "revocable... in a manner that allows immediate revocation."
- **Immediate revocation without a full access-token denylist:** each user record carries a `security_stamp` (a UUID/counter), embedded as a JWT claim at issuance. Password change, role change, forced logout, or account deactivation increments the stamp. `JwtAuthenticationFilter` compares the token's `securityStamp` claim to the current value on every request — a single indexed lookup, cached locally for 60 seconds per user to avoid a DB round-trip on every request (acceptable worst-case revocation latency: 60s, well inside the 15-minute access-token TTL it's backstopping). *Judgment call:* a full per-token access-token denylist (checked on every request) was rejected as unnecessary operational complexity for this scale (NFR-SCALE-02: 100 concurrent users) — the security-stamp approach gives near-immediate revocation at a fraction of the infrastructure cost and needs no external cache (Redis) for a single-instance deployment.
- **Logout:** revokes the presented refresh token (and, if "log out everywhere" is chosen, all refresh tokens for the user plus a `security_stamp` bump).

## 2.2 LDAP/Active Directory Delegation

- Used **only at login time** (`POST /api/v1/auth/login`), never per-request — once authenticated, the user carries an IAMS-issued JWT identical in shape to a local-auth token, so the rest of the system (filter chain, RBAC, SoD) is authentication-source-agnostic.
- Connection is **LDAPS only** (`ldaps://`, port 636); STARTTLS on 389 is not supported to avoid a plaintext-then-upgrade window. Configured via `spring-ldap` `LdapAuthenticationProvider` with a search-then-bind pattern: the read-only service account (scoped to the minimum required OU, per SRS 6.6) searches for the user's DN by username, then a second bind attempt is made with the user's own submitted password — the service account's own credential never substitutes for the user's.
- **Just-in-time provisioning:** on first successful LDAP bind, a shadow local `users` row is created/updated (email, display name), and LDAP group membership is mapped to IAMS roles via an Administrator-configured `ldap_group_role_mapping` table — group membership is *never* trusted to directly imply IAMS permissions without this explicit mapping.

## 2.3 SAML 2.0 / OIDC SSO

- Implemented via Spring Security's `saml2Login` (SAML2) and `oauth2Login`/OIDC client support.
- **SAML2 validation checklist enforced on every assertion**, per SRS 6.6 ("no implicit trust of unsigned attributes"):
  1. Signature validated against the IdP's published metadata certificate.
  2. Assertion decrypted if the IdP encrypts (preferred; configurable per IdP capability).
  3. `Audience` restriction equals this deployment's SP Entity ID.
  4. `Issuer` matches the configured IdP entity ID.
  5. `NotBefore`/`NotOnOrAfter` validated against server clock (with a small, configurable skew tolerance, default 60s).
  6. `InResponseTo` matched against a stored, single-use `AuthnRequest` ID (replay protection) — a request ID is deleted from the store the moment it's consumed.
  7. Only attributes carried **inside the signed/encrypted assertion** are trusted for role mapping; any unsigned relay-state or query parameter is ignored for authorization purposes.
- **OIDC validation:** standard Authorization Code + PKCE flow; `id_token` signature validated against the IdP's JWKS (cached, refreshed on `kid` miss); `iss`/`aud`/`exp`/`nonce` validated per OIDC Core spec.
- Same JIT provisioning + group/claim-to-role mapping pattern as LDAP (Section 2.2).

## 2.4 Super Administrator Local-Auth Fallback

SRS 6.1 requires that an unreachable IdP never cause full lockout. Implementation:

- At least one Super Administrator account is provisioned at first boot (seeded via an interactive `docker compose run iams-backend --seed-admin` step or an env-var-provided initial password that **must** be rotated on first login) and is permanently flagged `authSource = LOCAL`.
- The login endpoint **always** accepts the local username/password grant for any account flagged `LOCAL`, regardless of whether LDAP/SSO is configured as the organization-wide default — LDAP/SSO configuration only changes which grant type is *offered first* in the UI, never which grants the backend *accepts*.
- The Administrator UI explicitly blocks converting the **last remaining** local Super Administrator account to LDAP/SSO-only, and blocks disabling local authentication entirely while that condition holds — a hard server-side invariant, not a UI suggestion, so a future admin can't accidentally lock the organization out by misconfiguring SSO.

## 2.5 Password Hashing and Policy

- **Argon2id** (Spring Security's `Argon2PasswordEncoder`), OWASP's current primary recommendation, memory-hard against GPU/ASIC cracking. Default parameters: `t=2` (iterations), `m=19456` KB (~19 MB), `p=1` (parallelism) — OWASP Password Storage Cheat Sheet's current baseline. Configurable down for very small/low-RAM hosts (documented trade-off), with **bcrypt** (`cost=12`) offered as an explicit fallback via `iams.security.password-encoder=bcrypt` for organizations running IAMS on genuinely constrained hardware. Plaintext passwords are never logged (enforced by a `@Sensitive` field-masking Jackson mixin applied globally to request/response logging, Section 11).
- **FR-SEC-05 policy knobs**, all configurable, defaults: minimum length 12, complexity (at least 3 of 4 character classes), reuse prevention against the last 5 password hashes. **Forced periodic expiry defaults to OFF (0 = never)** — *judgment call*: NIST SP 800-63B explicitly recommends against mandatory periodic rotation (it drives predictable password patterns) in favor of length + breach-triggered rotation; FR-SEC-05 requires the *capability* to configure expiry, not that it be on by default, so IAMS ships the NIST-aligned default and lets an organization whose compliance regime mandates periodic expiry (e.g., a specific state/sector policy) turn it on.

## 2.6 Optional TOTP Two-Factor Authentication (FR-SEC-03)

Standard RFC 6238 TOTP (30-second step, SHA-1 per common authenticator-app compatibility), secret generated per user and shown as a QR code for enrollment (Google Authenticator/Authy/1Password compatible), 10 single-use backup codes issued at enrollment and re-issuable by an Administrator. Optional per-user or Administrator-mandated per-role (e.g., mandatory for Super Administrator and IT Security Officer accounts — recommended default for those two roles given their blast radius, configurable).

---

# 3. Authorization (AuthZ) Implementation

## 3.1 The Three Layers That Compose an Access Decision

Per SRS 6.2, "a user's effective permission is the intersection of their role's permissions and their assigned hierarchy scope" — and per the task's system facts, SoD is a *third*, distinct layer on top of that intersection:

1. **RBAC** — does any role assignment held by this user grant the permission string this action requires (e.g., `AUDIT_APPROVE`, `ASSET_WRITE`, `USER_MANAGE`)?
2. **Org-hierarchy scope** — is the specific entity being acted on within the subtree rooted at one of the org nodes that role assignment covers (FR-USR-04)?
3. **Separation of Duties** — for the specific subset of actions FR-USR-06/FR-AUD-22 guard, is the acting user also the entity's creator/submitter, and if so, does an active waiver (FR-USR-09) cover this action type in this scope?

A request must clear all three, in that order, to be authorized. Layer 3 is the one that makes IAMS's authorization more than textbook RBAC: **a user can hold the `AUDIT_APPROVE` permission, be correctly scoped to the department, and still be denied — because they submitted *this* audit.**

## 3.2 Method Security Annotations vs. a Centralized Policy Evaluator — Decision

**Decision: a centralized policy evaluator (`AccessPolicyEvaluator`), invoked declaratively via `@PreAuthorize` method-security annotations on application-service methods.** Neither pure option was acceptable alone:

- Pure scattered `@PreAuthorize("hasRole('AUDITOR') and #org.isWithinScope(...)")` SpEL expressions become unmaintainable the moment a third concern (SoD, with its waiver-override exception) has to be composed in — SpEL is not unit-testable in isolation, and duplicating the SoD lookup logic across a dozen annotations is exactly how a self-approval bypass gets introduced later.
- A purely imperative "call a check method at the top of every service method" approach is easy to forget — it fails *open* by omission, which is unacceptable for a control the BRD explicitly calls out as previously "unenforced in the system" (BRD 2.1).

The hybrid keeps the AOP enforcement guarantee (a `@PreAuthorize`-annotated method **cannot execute** without the evaluator returning `true` — it fails closed, and is visible via a simple `grep` audit of the codebase) while keeping all three layers' actual logic in one unit-testable Spring bean:

```java
@Service
public class AccessPolicyEvaluator {
    public boolean can(Authentication auth, String permission, ScopedEntityRef target) {
        if (!rbac.hasPermission(auth, permission)) return false;                 // layer 1
        if (!orgScope.isWithinAssignedScope(auth, target.orgNodeId())) return false; // layer 2
        if (sod.isGuardedAction(permission)) {                                    // layer 3
            boolean isSelfApproval = sod.actorIsCreator(auth, target);
            if (isSelfApproval && !sod.hasActiveWaiver(target.orgNodeId(), permission)) {
                auditLog.record(SOD_SELF_APPROVAL_BLOCKED, auth, target);
                return false;
            }
            if (isSelfApproval) {
                auditLog.recordViaWaiver(auth, target, permission); // FR-USR-09: logged distinctly
            }
        }
        return true;
    }
}
```

invoked as `@PreAuthorize("@accessPolicy.can(authentication, 'AUDIT_APPROVE', #auditRef)")` on the **application-service layer**, per SRS 2.2 ("Application/Service Layer: ... authorization checks") — not on the controller — so an internal caller (a scheduled job, another service) cannot bypass the check by skirting the HTTP layer. This is consistent with OWASP ASVS 4.0 V1.2/V4.1's recommendation for a centralized, testable authorization component.

**Reconciliation with the Backend spec (IAMS-BAS-1.0, Section 5.1):** that document independently designed SoD as a standalone `@EnforceSeparationOfDuties` / `@Aspect` mechanism triggered separately from RBAC/scope checks. Run together as literally written, both mechanisms would fire on the same approval call — two waiver lookups, two audit-log entries, two exception paths. Resolution (recorded in BAS Section 5.1.1): `AccessPolicyEvaluator` above is the sole trigger; the Backend spec's standalone aspect is dropped. What carries over from it, and what layer 3 (`sod.actorIsCreator(...)` / `sod.hasActiveWaiver(...)` above) actually implements: the `CreatorIdentityResolver<T>` interface (BAS Section 5.1) resolved per target entity type, plus the `SoDWaiverRepository` and `SecurityActivityLogService` beans it references — reuse those concrete types when implementing `sod.actorIsCreator` and `sod.hasActiveWaiver` rather than inventing new ones.

## 3.3 FR-AUD-22's Routing Nuance

FR-AUD-22 is not a simple deny: when the Auditor and the scoped Department Head are the same individual under an active SoD Waiver, the system must route approval to the Super Administrator or a configured alternate approver rather than either silently allowing self-approval or permanently stalling the audit. This is implemented as workflow logic sitting one level above the policy evaluator: the audit-submission service calls `accessPolicyEvaluator.can(...)` for the normal approver; if it returns `false` **and** the reason is "sole eligible approver is the submitter under an active waiver" (a distinguishable outcome from "no waiver, hard block"), the workflow automatically re-routes the pending-approval record to `organization.alternateApprover` (configured per FR-USR-09's waiver scope) or the Super Administrator, and notifies them (FR-NTF-04 pattern) — the audit is never left in a state where nobody can close it.

## 3.4 Step-Up Re-Authentication

Gated action set (SRS 6.2 / FRS FR-SEC-06): permission/role changes, bulk export, retention-policy changes, legal-hold changes — plus, by extension of the same rationale, SoD Waiver creation/modification (FR-USR-09) and deactivating the last local Super Administrator account.

**Mechanism:**

1. Client calls a `@RequiresStepUp`-annotated endpoint without a fresh step-up assertion → `StepUpRequirementInterceptor` (filter chain #10) returns `403 {"code":"STEP_UP_REQUIRED"}`.
2. Client re-prompts for the user's password (or TOTP code if enrolled) and calls `POST /api/v1/auth/step-up`.
3. On success, the server issues a **step-up assertion**: a claim (`stepUpAt`) added to a freshly re-issued access token, valid for 5 minutes (configurable via `iams.security.step-up.ttl`).
4. Client retries the original request; the interceptor checks `now - stepUpAt <= ttl`.
5. Every step-up success/failure is itself a SEC log event (Section 11), since a step-up prompt is inherently a signal something sensitive is about to happen.

## 3.5 Break-Glass Emergency Access (FR-SEC-16) — Flagged, Not Yet Designed

**Gap identified during v1.1 baseline reconciliation:** IAMS-FRS-2.0's FR-SEC-16 requires a time-boxed (default 4 hours) Super Administrator emergency-elevation path — mandatory reason captured on activation, immediate dual notification (the IT Security Officer plus at least one other Administrator), and every action taken during the elevation window flagged distinctly in the immutable activity log. This document's v1.0 baseline predates FR-SEC-16 and has no corresponding filter, endpoint, or workflow above. It is called out here rather than silently left absent because it sits squarely in this document's authorization/audit surface — implementation should most likely reuse the step-up re-authentication mechanism (Section 3.4) as the activation gate, the SEC log's structured event schema (Section 11) for the dual notification and per-action flag, and the `security_stamp`/TTL machinery (Section 2.1) to enforce the hard expiry, rather than introducing a parallel elevation mechanism — but the concrete design is deferred to a future revision of this document. See IAMS-FRS-2.0, FR-SEC-16 for the authoritative requirement.

---

# 4. CORS / CSRF Posture

**Architecture:** IAMS is a React SPA authenticated via `Authorization: Bearer <JWT>` — not cookie-based session auth for API calls. This choice has a direct, favorable CSRF consequence, and one narrow exception that needs its own control.

## 4.1 The Decision

- **Access token:** held in memory only (a JS variable, never `localStorage`/`sessionStorage`, to reduce persistent-XSS token-theft blast radius) and attached as an `Authorization` header on every API call.
- **Refresh token:** delivered as an **HttpOnly, Secure, `SameSite=Strict`** cookie, scoped to the `/api/v1/auth/refresh` path only (not sent on any other request), because a refresh token that lived in JS-readable storage would be a strictly worse XSS target than a short-lived access token.
- **CSRF protection is disabled (`csrf(AbstractHttpConfigurer::disable)`) for the entire bearer-token-authenticated API surface.** *Rationale:* CSRF is fundamentally a session-riding attack — it works because the browser attaches an ambient credential (a cookie) automatically to a cross-site request the attacker's page didn't need to read. An `Authorization` header is never attached automatically by the browser; a cross-site attacker page cannot read it (Same-Origin Policy) and therefore cannot forge a bearer-authenticated request. This aligns with the OWASP CSRF Prevention Cheat Sheet's guidance that token-header-authenticated APIs generally do not need CSRF tokens.
- **CSRF protection remains explicitly enabled for the two cookie-authenticated endpoints** — `/api/v1/auth/refresh` and `/api/v1/auth/logout` — via `CookieCsrfTokenRepository` (double-submit pattern) **plus** `SameSite=Strict` on the refresh cookie (which alone blocks the cross-site request in all modern browsers) **plus** an `Origin`/`Referer` header check as defense in depth. This is the one place a cookie is genuinely ambient, so it's the one place CSRF is a real threat.

## 4.2 CORS Configuration

Because the recommended production topology serves the frontend and backend under **one origin** (`iams-reverse-proxy` routes both `/` → static frontend and `/api/*` → `iams-backend`), CORS is **off by default in production** (empty allow-list = same-origin only). It is explicitly configurable for:

- Staging/local development, where the React dev server runs on a different port.
- Organizations that deliberately split frontend and backend across subdomains.

```yaml
iams:
  cors:
    allowed-origins: ""   # comma-separated; empty = same-origin only (production default)
    allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
    allowed-headers: Authorization,Content-Type,X-Correlation-Id
    allow-credentials: false   # true only if a cross-origin deployment also needs the refresh cookie — discouraged; same-origin sidesteps this entirely (BRD 11.2 simplicity)
```

---

# 5. Secrets Management

## 5.1 Pragmatic Default: SOPS-Encrypted `.env` + Docker Compose File Secrets

Given the target operator (BRD 11.2: no dedicated ops team, plain `docker compose up`, not Kubernetes), the primary path avoids requiring a running secrets-manager *service*:

- **Highest-value secrets (DB password, JWT signing key, MinIO root credential, LDAP/SSO service-account credentials, webhook HMAC keys)** are delivered as **Docker Compose file-based secrets** (`secrets:` top-level key, `file:` source) — this works in plain `docker compose up` without Swarm mode (Compose 1.27+), mounts each secret as a file under `/run/secrets/<name>` inside the container, and — critically — keeps the value **out of `docker inspect`/process-environment exposure**, unlike a plain env var.
- **The broader configuration set** (integration feature flags, non-critical settings, anything an operator legitimately needs to eyeball or diff) is an `.env` file encrypted at rest with **SOPS** (Mozilla's Secrets OPerationS) using **age** (simpler key model than GPG for a small IT team — a single keypair, no web-of-trust ceremony). The repository ships `.env.sops.yaml` (encrypted, safe to commit) and `.env.example` (structure documentation, no real values); a documented one-line `sops -d .env.sops.yaml > .env` decrypts it locally before `docker compose up --env-file .env`, and the plaintext `.env` is git-ignored.
- Both mechanisms are **file-based**, not a running service — nothing new to patch, back up, or keep available. This is the primary recommendation.

## 5.2 Optional Vault Integration Point

For organizations sophisticated enough to already run HashiCorp Vault (or willing to stand it up), the backend defines a `SecretsProvider` SPI with two implementations selected by `iams.secrets.provider`:

```
iams.secrets.provider=file   # default: reads /run/secrets/* and the decrypted .env
iams.secrets.provider=vault  # opt-in: Spring Cloud Vault starter, KV v2 engine, AppRole auth
```

Vault is never the only option (it would contradict BRD 11.2), and its absence never blocks go-live — it is purely an upgrade path for an organization that already has the operational muscle to run it.

## 5.3 Never Committed, Never Logged

`.gitignore` excludes `.env`, `secrets/*.txt`, `secrets/*.pem` at the repository root. The structured-logging Jackson mixin (Section 11) masks any field annotated `@Sensitive` (passwords, API keys, tokens) regardless of log level, and the request/response logging filter (Section 1.3, filter #12) never logs request/response *bodies* for endpoints under `/api/v1/auth/**` or any endpoint accepting a credential field.

## 5.4 Rotation Procedure Per Credential Type

| Credential | Rotation Procedure |
|---|---|
| DB password (`iams_app` role) | `ALTER USER iams_app WITH PASSWORD '<new>'` in Postgres → update the secret file → `docker compose up -d iams-backend` (brief restart; acceptable within the 4-hour RTO budget, not a live-rotation requirement) |
| JWT signing key | Publish the new keypair alongside the old one (`kid`-keyed key set); backend accepts tokens signed by either `kid` for a grace window equal to the access-token TTL (15 min default); after the grace window, retire the old key |
| LDAP/SSO service-account password | Update in the org's directory + update the secret file + restart `iams-backend` (or hot-reload if `iams.secrets.provider=vault` with Spring Cloud Vault's refresh-scope) |
| Integration API keys / webhook HMAC signing keys | Dual-key overlap: the Integration Gateway (Section 8) accepts either the outgoing or incoming key during a configured overlap window, so the counterparty system can rotate on its own schedule without a synchronized cutover |
| MinIO application access key | Rotate via `mc admin user` against the scoped application user (never the root credential, which is used only once at bootstrap to create that scoped user — Section 7) |

---

# 6. Rate Limiting

## 6.1 Two Tiers, By Design

| Tier | Where | Basis | Purpose |
|---|---|---|---|
| Coarse | `iams-reverse-proxy` (Nginx) | Source IP | Pre-auth flood/credential-stuffing protection — cheap, runs before any JWT parsing |
| Fine-grained | `iams-backend` (`RateLimitAccountingFilter`) | Authenticated user ID or API key ID | The actual NFR-API-01 requirement — "per authenticated user and per API key" is, by definition, an identity-aware decision the proxy can't cheaply make without a non-stock module |

## 6.2 Nginx Configuration (Coarse Tier)

```nginx
limit_req_zone $binary_remote_addr zone=iams_per_ip:10m rate=20r/s;

server {
    location /api/ {
        limit_req zone=iams_per_ip burst=40 nodelay;
        limit_req_status 429;
        proxy_pass http://iams_backend_upstream;
        proxy_set_header X-Correlation-Id $request_id;
    }
}
```

## 6.3 Application-Layer Bucket (Fine-Grained Tier)

Bucket4j token-bucket algorithm, in-memory (Caffeine-backed) for the default single-instance deployment — *judgment call*: a distributed store (Redis) is unnecessary complexity at the documented scale (NFR-SCALE-02: 100 concurrent users, single backend instance) and would be a new operational dependency the target persona doesn't need. The upgrade path is documented for when NFR-SCALE-02's "read replicas / horizontal scaling" path is actually taken: swap Caffeine for a Redis- or Postgres-table-backed Bucket4j proxy manager so buckets are shared across replicas.

Default thresholds (configurable per deployment):

| Principal | Default limit |
|---|---|
| Authenticated user (general API) | 120 requests/minute, burst 30 |
| Authenticated user (bulk export/report generation endpoints) | 5 requests/minute |
| API key (integration service account) | 300 requests/minute, burst 60 |

## 6.4 429 Contract

```
HTTP/1.1 429 Too Many Requests
Retry-After: 37
Content-Type: application/json

{"code":"RATE_LIMITED","message":"Too many requests. Retry after 37 seconds.","correlationId":"..."}
```

Every violation is logged as a structured SEC-adjacent event (`RATE_LIMIT_REJECTED`, Section 11) with the principal, endpoint, and tier that rejected it — satisfying NFR-API-02's "logged, not a silent failure."

---

# 7. Docker Compose Topology

## 7.1 Network Segmentation

Two networks enforce Decision #2 (database/object-store never externally reachable):

- **`iams-edge`** — `iams-reverse-proxy` ↔ `iams-frontend`/`iams-backend`. This is the only network with a published host port.
- **`iams-internal`** — `iams-backend` ↔ `iams-db`, `iams-object-store`. Declared `internal: true`, meaning Docker refuses it any route to the host's network at all — `iams-db` and `iams-object-store` are unreachable even if a port were accidentally published, because the network itself has no external gateway.

## 7.2 `docker-compose.yml` (Production)

```yaml
networks:
  iams-edge:
    driver: bridge
  iams-internal:
    driver: bridge
    internal: true

volumes:
  iams-db-data:
  iams-minio-data:
  iams-backups:
  iams-wal-archive:

secrets:
  db_password:
    file: ./secrets/db_password.txt
  jwt_signing_key:
    file: ./secrets/jwt_signing_key.pem
  minio_root_password:
    file: ./secrets/minio_root_password.txt
  ldap_bind_password:
    file: ./secrets/ldap_bind_password.txt

services:
  iams-reverse-proxy:
    image: nginx:1.27-alpine
    ports:
      - "443:443"
      - "80:80"          # redirects to 443 only; no plaintext service traffic
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/certs:/etc/nginx/certs:ro
      - ./frontend-dist:/usr/share/nginx/html:ro   # large-deployment topology; omit for small
    networks: [iams-edge]
    depends_on:
      iams-backend: { condition: service_healthy }
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:80/healthz"]
      interval: 30s
      timeout: 5s
      retries: 3
    restart: unless-stopped

  iams-backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: production
      DB_HOST: iams-db
      DB_PORT: "5432"
      DB_NAME: iams
      DB_USER: iams_app
      DB_PASSWORD_FILE: /run/secrets/db_password
      JWT_SIGNING_KEY_FILE: /run/secrets/jwt_signing_key
      OBJECT_STORE_ENDPOINT: http://iams-object-store:9000
      OBJECT_STORE_ACCESS_KEY: iams-backend-app
      OBJECT_STORE_SECRET_KEY_FILE: /run/secrets/minio_root_password   # bootstrap only — see 7.4
      LDAP_URL: ldaps://ad.example.org:636
      LDAP_BIND_PASSWORD_FILE: /run/secrets/ldap_bind_password
      LOG_FORMAT: json
      IAMS_SERVE_FRONTEND: "false"   # "true" for small-deployment topology (7.5)
    secrets: [db_password, jwt_signing_key, minio_root_password, ldap_bind_password]
    networks: [iams-edge, iams-internal]
    depends_on:
      iams-db: { condition: service_healthy }
      iams-object-store: { condition: service_healthy }
    healthcheck:
      test: ["CMD", "curl", "-fsS", "http://localhost:8080/actuator/health/readiness"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 60s
    deploy:
      resources:
        limits: { cpus: "2.0", memory: 2G }   # small deployment guidance — see 7.6
    restart: unless-stopped

  iams-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: iams
      POSTGRES_USER: iams_app
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    secrets: [db_password]
    volumes:
      - iams-db-data:/var/lib/postgresql/data
      - iams-wal-archive:/var/lib/postgresql/wal-archive
      - ./postgres/postgresql.conf:/etc/postgresql/postgresql.conf:ro
    networks: [iams-internal]        # NOT on iams-edge — unreachable from proxy or host
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U iams_app -d iams"]
      interval: 10s
      timeout: 5s
      retries: 5
    deploy:
      resources:
        limits: { cpus: "1.0", memory: 1G }   # small deployment guidance — see 7.6
    restart: unless-stopped

  iams-object-store:
    image: minio/minio:RELEASE.2024-01-01T00-00-00Z
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: iams-minio-bootstrap
      MINIO_ROOT_PASSWORD_FILE: /run/secrets/minio_root_password
    secrets: [minio_root_password]
    volumes:
      - iams-minio-data:/data
    networks: [iams-internal]        # NOT reachable from proxy or host — backend proxies all object access (SRS 4.2)
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 15s
      timeout: 5s
      retries: 5
    deploy:
      resources:
        limits: { cpus: "0.5", memory: 512M }   # small deployment guidance — see 7.6
    restart: unless-stopped

  iams-backup-agent:
    build: ./backup-tools     # ships restore.sh / restore.ps1, backup.sh, wal-push.sh (Section 10)
    environment:
      DB_HOST: iams-db
      RPO_MINUTES: "15"
    secrets: [db_password, minio_root_password]
    volumes:
      - iams-backups:/backups
      - iams-wal-archive:/var/lib/postgresql/wal-archive:ro
    networks: [iams-internal]
    restart: unless-stopped
```

## 7.3 Deployment-Scale Sizing Guidance

| Resource | Small (~500 assets, <10 concurrent users) | Large (100,000+ assets, 100 concurrent users — NFR-SCALE-01/02) |
|---|---|---|
| `iams-backend` CPU/RAM | 1.0 vCPU / 1–2 GB | 4.0 vCPU / 6–8 GB; consider a second replica behind the reverse proxy once horizontal scaling is warranted |
| `iams-db` CPU/RAM | 1.0 vCPU / 1 GB, `shared_buffers=256MB`, `max_connections=50` | 4.0 vCPU / 8 GB+, `shared_buffers=2GB`, `max_connections=150`, dedicated fast disk (SSD/NVMe) for WAL |
| HikariCP pool size | 10 | 30–50 (tuned against `max_connections`, leaving headroom for `iams-backup-agent` and any read-replica connections) |
| `iams-object-store` | 0.5 vCPU / 512 MB, disk sized to attachment volume (a few GB) | 2.0 vCPU / 4 GB, disk sized generously for 100k+ assets' images/attachments — plan for multi-hundred-GB growth |
| Deployment topology | `iams-backend` serves the frontend statically (`IAMS_SERVE_FRONTEND=true`); `iams-frontend`/Nginx static-serving stage can be skipped entirely | Dedicated `iams-frontend` static-serving stage in `iams-reverse-proxy`, separate CDN-free asset caching headers |

## 7.4 MinIO Bootstrap Note

The `MINIO_ROOT_*` credential is used **once**, at first boot, by an init step (`mc admin user add` / `mc admin policy attach`) to create a **scoped** application user (`iams-backend-app`) with a bucket-restricted IAM policy — the root credential itself is never the one `iams-backend` authenticates with day to day, consistent with least-privilege (SRS 6.6 pattern applied internally, not just to external integrations).

## 7.5 Small vs. Large: Frontend Serving Mode

Per SRS 2.5 ("served via Nginx, or served statically by the backend for small deployments"): `IAMS_SERVE_FRONTEND=true` has `iams-backend` serve the built React bundle directly from a classpath resource path, letting a very small deployment drop the frontend-serving concern from `iams-reverse-proxy` entirely (Nginx becomes a pure TLS-terminating reverse proxy). Large deployments set it `false` and let `iams-reverse-proxy` serve static assets with proper cache headers, keeping static-file I/O off the JVM.

## 7.6 Staging vs. Production

Per SRS 2.6, every deployment provisions Staging in addition to Production:

- A `docker-compose.staging.yml` override (invoked as `docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d`) trims resource `limits` (Section 7.3's small-deployment column is the Staging default regardless of Production's tier), points at **separate** named volumes (`iams-db-data-staging`, etc. — never shared with Production, never a symlink to Production data outside of an explicit restore operation), and uses a separate TLS certificate (an internal-CA or self-signed cert is acceptable for Staging).
- Staging is the mandatory target for: Flyway migration validation before Production (NFR-MAINT-02), version-upgrade testing with a production-equivalent data snapshot (NFR-MAINT-05), and the quarterly restore-test drill (Section 10.4) — restoring a Production backup into Staging is the actual verification mechanism for NFR-AVAIL-04, not a separate throwaway environment.

---

# 8. Integration Gateway Design

## 8.1 Adapter Pattern, One Enforcement Point

Per SRS 2.3, the Integrations module (INT) is "the single point through which any external system communicates with IAMS" — implemented as a hexagonal adapter layer so every cross-boundary control (FR-SEC-09, FR-INT-06, FR-SEC-14, FR-INT-05, BRD 6.5) is enforced **once**, in the gateway, not re-implemented per integration:

```java
public interface IntegrationAdapter {
    IntegrationType type();                 // LDAP, SSO, ERP_EXPORT, HR_SIS_SYNC, SMS, WEBHOOK
    boolean healthCheck();
    IntegrationResponse invoke(IntegrationRequest request);
}
```

Concrete adapters: `LdapAdapter`, `SsoAdapter`, `ErpExportAdapter`, `HrSisSyncAdapter`, `SmsGatewayAdapter`, `WebhookDispatchAdapter`. All calls pass through `IntegrationGatewayService`, which enforces, in order, before any adapter method runs:

1. **Enable/disable gate (FR-INT-05):** `integration_config.enabled` — every integration defaults to `false`; the gateway refuses to invoke a disabled adapter regardless of any other configuration state.
2. **Compliance sign-off gate (BRD 6.5):** `integration_config.compliance_signoff_by` / `compliance_signoff_at` must both be populated — an Administrator flipping `enabled=true` alone is **not sufficient**; `IntegrationGatewayService.isActivatable()` checks both flags and refuses to invoke (or even health-check) an adapter missing sign-off, mirroring UC-INT-01's flow where Priya must confirm sign-off before the Finance Officer's export functions.
3. **Credential retrieval via `SecretsProvider` (Section 5):** each adapter is issued its own scoped credential — never a blanket admin credential (FR-SEC-14).
4. **Transport authentication via a shared `SecureHttpClient` wrapper:** every adapter is required to route outbound calls through this wrapper, which enforces mutual TLS or signed-credential auth generically (FR-SEC-09) — an adapter cannot opt out and use a raw HTTP client, because `IntegrationAdapter` implementations are constructor-injected only with `SecureHttpClient`, never a general-purpose one.

## 8.2 Retry / Circuit-Breaker Policy (Resilience4j)

| Policy | Configuration | Rationale |
|---|---|---|
| Retry | 3 attempts, exponential backoff (500ms/1s/2s), only on transient failures (timeout, 5xx) — never on 4xx (a bad credential retried 3× just triggers lockouts on the far end) | Outbound calls to ERP/HR-SIS/SMS systems the org doesn't control |
| Circuit Breaker | Opens at ≥50% failure rate over a 10-call sliding window; 30s open state; half-open probe of 2 calls | Prevents a slow/down external system from cascading into IAMS request-thread exhaustion |
| Bulkhead | Max concurrent calls per integration capped (default 5) | A slow HR/SIS sync must never starve threads needed for interactive user requests |

## 8.3 Outbound Webhooks: Allow-List + HMAC Signing (FR-INT-06, NFR-SEC-11)

- **Allow-list:** `webhook_allowlist` table, Administrator-managed only, **step-up required** to add/change an entry (Section 3.4) — this is the SSRF prevention control (SRS 6.6, NFR-SEC-11): outbound webhook URLs must be Administrator-registered and allow-listed, and the API never accepts ad hoc callback URLs in request bodies.
- **SSRF defense in depth:** the allow-listed hostname's resolved IP is validated at **dispatch time** (not just at allow-list creation time) to reject private/loopback/link-local ranges (defends against DNS rebinding between allow-listing and delivery); HTTP redirects are not followed unless the redirect target is itself on the allow-list.
- **Signing:** payload signed HMAC-SHA256 with a per-endpoint signing key (from `SecretsProvider`); `X-IAMS-Signature` and `X-IAMS-Timestamp` headers let the receiver verify authenticity and reject replayed deliveries (timestamp outside a 5-minute window is rejected by convention on the receiving side, documented in the Administrator Guide's webhook integration contract).
- **Delivery logging:** every attempt (success, failure, retry) is written to the immutable activity log, extending the FR-MIG-04 logging pattern to the INT module per SRS 6.6's table.

## 8.4 R1 vs. R3 Sequencing

Per FRS Section 5, the ERP/HR-SIS/SMS/Webhook adapters ship in R3. The **framework** (`IntegrationAdapter` interface, `IntegrationGatewayService`, `SecureHttpClient`, the enable/disable+sign-off gate, Resilience4j wiring) is built in R1 — not deferred — because the **LDAP and SSO adapters use this exact same framework and are required from R1** (BR-07 is an R1 requirement, FRS FR-SEC-02). This means the Integration Gateway's security controls are proven in production by LDAP/SSO traffic a full release before the ERP/HR-SIS/SMS/Webhook adapters are built on top of it, directly satisfying the task's instruction that "its underlying framework... should be designed now so it isn't retrofitted insecurely later."

---

# 9. CI/CD Pipeline

**Assumption stated explicitly:** this pipeline assumes access to *some* self-hostable CI runner (GitHub Actions' free tier, a self-hosted Gitea Actions/Woodpecker/Jenkins instance, or GitHub Actions with a self-hosted runner for organizations whose policy forbids any cloud CI touching their build). It does **not** assume every deploying organization has a paid SaaS CI vendor available — every scanning tool below is free, open-source, and runnable entirely offline, so the pipeline itself introduces no new mandatory external dependency (consistent with the "no hard dependency on any external SaaS/cloud service" constraint, SRS Section 8).

| Stage | Tooling | Gate |
|---|---|---|
| 1. Build | `mvn -B clean verify` (backend), `npm ci && npm run build` (frontend) | Compile failure blocks |
| 2. Unit tests | JUnit 5 / Mockito (backend), Jest + React Testing Library (frontend) | Any failure blocks (NFR-MAINT-03) |
| 3. Integration tests | Testcontainers (ephemeral Postgres + MinIO), Spring Boot `@SpringBootTest` slices — **must include a concurrency test simulating parallel Stock Transfers** to validate the atomic `UPDATE ... WHERE quantity >= :n` pattern under contention (NFR-CONC-02) | Any failure blocks |
| 4. SAST | Semgrep OSS (OWASP Top 10 rule pack, self-hostable, free) — SonarQube Community Edition optional add-on for deeper quality gates | High-severity finding blocks |
| 5. Dependency vulnerability scan (FR-SEC-12) | OWASP Dependency-Check (Maven plugin, backend) + `npm audit`/`osv-scanner` (frontend) | High/Critical (CVSS ≥ 7.0) blocks unless an entry exists in `dependency-check-suppressions.xml`, which itself requires IT Security Officer review per the remediation SLA (Section 9.1) |
| 6. Container image build | Multi-stage Dockerfile, `eclipse-temurin:21-jre-alpine` (or distroless) base, non-root `USER iams` | — |
| 7. Container image scan | Trivy (self-hosted CLI, free) — OS packages + app dependencies inside the built image | High/Critical blocks |
| 8. Push to registry | Self-hosted registry (Harbor or a plain `registry:2` container) or an org's existing container registry | — |
| 9. Deploy to Staging | `docker compose pull && docker compose up -d` against the Staging host; Flyway runs automatically on `iams-backend` startup against the Staging DB (`spring.flyway.enabled=true`) — **this is the Flyway migration-to-Staging gate** | Scripted smoke test (health endpoint + a handful of key-endpoint checks) must pass |
| 10. Manual promotion approval | A human (Super Administrator or IT Security Officer) approves promotion — deliberately manual, not auto-promoted, given the target org's preference for a human checkpoint before touching Production data | Approval required |
| 11. Deploy to Production | Same `pull`/`up` pattern; Flyway migration runs against Production **only after** the identical migration succeeded in Staging (NFR-MAINT-02) | — |

## 9.1 Remediation SLA (FR-SEC-12)

Critical: 7 days. High: 30 days (matches BRD Section 10's general security-patch cadence). Medium/Low: tracked, not release-blocking. An IT Security Officer risk-acceptance is required and logged for any exception that misses the SLA.

## 9.2 Rollback

Flyway Community Edition has no built-in `undo` migration. Rollback strategy is therefore **redeploy the prior image tag against the same (forward-compatible) schema**, per NFR-MAINT-05's requirement that each release remain backward-compatible with the prior schema during a migration window — not a schema rollback. If a migration is genuinely destructive (rare, and flagged in code review), the documented fallback is a restore from the pre-migration backup via the scripted restore tool (Section 10).

---

# 10. Backup, Restore & Disaster Recovery

## 10.1 WAL Archiving (RPO 15 Minutes — NFR-AVAIL-02)

PostgreSQL `archive_mode=on`, `archive_command` pushes each completed WAL segment to `iams-wal-archive` (local volume) **and** mirrors it to a MinIO bucket (`iams-wal-archive`) for redundancy beyond the local disk. Combined with a nightly `pg_basebackup` (02:00 local time, configurable) retained per a configurable policy (default 30 daily + 12 monthly), this gives continuous point-in-time recovery capability with a maximum data-loss window bounded by WAL segment completion frequency — well inside 15 minutes under normal write volume.

## 10.2 Object Store Backup

MinIO data is mirrored nightly (`mc mirror`) to a secondary location — a second on-prem MinIO instance if the organization has one, otherwise an external USB/NAS target — on the same schedule and retention as the database backup, since asset attachments/images (SRS 4.2) carry the same RPO target.

## 10.3 `restore.sh` / `restore.ps1` — Scripted, Single-Command Restore (NFR-AVAIL-03)

```
./restore.sh --target production --point-in-time "2026-07-08T14:00:00Z" --confirm
./restore.sh --target staging --latest --confirm          # used by the quarterly drill, Section 10.4
```

**Steps the tool performs, in order, halting with an actionable error at the first failed step:**

1. Stop `iams-backend` (prevents writes racing the restore).
2. **Verify backup-set integrity** against the SHA-256 checksum manifest written at backup time — a corrupted backup is refused here, not discovered mid-restore.
3. Restore the base backup and replay WAL to the requested `recovery_target_time` (or the latest available point if `--latest`).
4. Restore the matching-timestamp MinIO data set.
5. **Post-restore validation suite** (must all pass before the tool reports success): row counts on key tables compared against the manifest's recorded counts at backup time; a basic connectivity/`SELECT 1` check; a foreign-key integrity spot-check; confirmation that the Flyway `schema_version` table matches the expected application version.
6. Only after every validation passes: restart `iams-backend`, print `RESTORE SUCCESSFUL`, and append an entry to `/backups/restore-history.log` (operator, timestamp, target point-in-time, backup set used, validation results) — feeding the same immutable log stream as other administrative actions (FR-SEC-04).

If any step fails, the tool **does not silently proceed** — it leaves the system in a documented state and points the operator at the manual runbook fallback, per NFR-AVAIL-03's explicit instruction that the manual runbook is retained "as a documented fallback for the scripted tool's own failure modes, not as the primary path."

## 10.4 Automatable Quarterly Restore-Test (NFR-AVAIL-04)

A scheduled job in `iams-backup-agent` (a weekly timer that fires the actual drill quarterly, configurable) runs `restore.sh --target staging --latest --confirm` against the **Staging** environment (never Production), executes the identical validation suite as a live restore, and:

- Writes a `backup_verification_log` row (timestamp, backup set restored, each validation's pass/fail, overall result).
- Sends a notification (FR-NTF pattern) to the Administrator and IT Security Officer with the outcome.

This directly satisfies NFR-AVAIL-04's "an actual restore test... not just a checkbox" — the test is a real restore into a real (if lower-capacity) environment with real validation, not a log line asserting "backup completed," and it happens whether or not a human remembers to schedule it.

---

# 11. Observability

## 11.1 Structured JSON Log Format

```json
{
  "timestamp": "2026-07-09T14:22:07.481Z",
  "level": "INFO",
  "correlationId": "b3e1c9a0-...",
  "userId": "usr_4821",
  "orgNodeId": "org_campus_north",
  "module": "AUD",
  "event": "SOD_SELF_APPROVAL_BLOCKED",
  "httpMethod": "POST",
  "path": "/api/v1/audits/{id}/approve",
  "status": 403,
  "durationMs": 42,
  "sourceIp": "10.20.4.11",
  "userAgent": "Mozilla/5.0 ...",
  "message": "User usr_4821 blocked from approving audit aud_9931 (self-submitted, no active waiver)",
  "exceptionClass": null,
  "exceptionMessage": null
}
```

Field list: `timestamp` (ISO 8601 UTC — NFR-I18N-02), `level`, `correlationId`, `userId` (nullable pre-auth), `orgNodeId` (nullable), `module` (FRS module code), `event` (a stable, machine-readable event code — e.g. `AUTH_LOGIN_SUCCESS`, `AUTH_LOGIN_FAILURE`, `SEC_PERMISSION_DENIED`, `SOD_SELF_APPROVAL_BLOCKED`, `SOD_WAIVER_ACTION`, `RATE_LIMIT_REJECTED`, `INTEGRATION_CALL_FAILED`, `RESTORE_COMPLETED`), `httpMethod`, `path`, `status`, `durationMs`, `sourceIp`, `userAgent`, `message`, `exceptionClass`/`exceptionMessage` (nullable, sanitized — never a raw stack trace or SQL string). Logback with `logstash-logback-encoder` (or `net.logstash.logback:logstash-logback-encoder`) emits this shape directly; `@Sensitive`-annotated fields are masked before serialization regardless of log level (Section 5.3).

## 11.2 Correlation ID Propagation

`iams-reverse-proxy` generates/passes through `X-Correlation-Id` (Section 1.2, R4) → `CorrelationIdFilter` (Section 1.3, filter #1) writes it to the SLF4J MDC → every log line for the request's lifetime carries it automatically via the Logback JSON encoder pattern → the response includes `X-Correlation-Id` so a helpdesk contact or Officer Reyes (UC-SEC-01) can quote it when investigating → outbound Integration Gateway calls (Section 8) add the same header, so a downstream ERP/webhook receiver's own logs can be cross-referenced → background jobs spawned from a request (bulk import, report generation, FR-MIG operations) capture the originating correlation ID into the job record, so a long-running async task's own log lines stay traceable to the request that started it even after the HTTP request itself completed — satisfying NFR-OBS-03's "trace a single user action end to end without direct database access."

## 11.3 Prometheus Metrics (Micrometer + `micrometer-registry-prometheus`, exposed at `/actuator/prometheus`)

| Metric | Type | Labels | Purpose |
|---|---|---|---|
| `iams_http_requests_total` | Counter | `method`, `path`, `status` | Request-rate/error-rate dashboards |
| `iams_http_request_duration_seconds` | Histogram | `method`, `path` | p50/p95/p99 latency — feeds NFR-PERF-01/05 SLA tracking |
| `iams_auth_login_attempts_total` | Counter | `outcome`, `source` (local/ldap/sso) | Auth health, feeds UC-SEC-01 investigation |
| `iams_authz_denials_total` | Counter | `permission`, `module` | RBAC/scope denial rate |
| `iams_sod_blocks_total` | Counter | `action` | Direct visibility into SoD control effectiveness (BR-21) |
| `iams_rate_limit_rejections_total` | Counter | `scope` (user/api_key/ip) | NFR-API-02 abuse visibility |
| `iams_async_job_queue_depth` | Gauge | `job_type` | NFR-OBS-02 async queue depth (bulk import, report gen, reconciliation) |
| `iams_async_job_duration_seconds` | Histogram | `job_type`, `outcome` | Async job SLA tracking (NFR-PERF-03/04) |
| `iams_db_connection_pool_active` / `_idle` | Gauge | `pool` | HikariCP saturation — critical given NFR-CONC-02's short-transaction requirement (Section 12) |
| `iams_integration_calls_total` | Counter | `integration`, `outcome` | Integration Gateway health |
| `iams_integration_circuit_breaker_state` | Gauge (0=closed,1=open,2=half-open) | `integration` | Resilience4j circuit visibility |
| `iams_backup_last_success_timestamp_seconds` | Gauge | — | Surfaces DR posture as a metric, not just a log line |
| `iams_backup_verification_last_result` | Gauge (1=pass, 0=fail) | — | Quarterly restore-test outcome (Section 10.4) |

## 11.4 Health-Check Endpoint Contract (NFR-OBS-01)

- `GET /actuator/health/liveness` — process-alive only, no dependency checks; used by the container runtime to decide whether to restart the container.
- `GET /actuator/health/readiness` — checks DB connectivity, MinIO connectivity, and disk-space threshold; used by `iams-backend`'s Compose `healthcheck:` directive and by `iams-reverse-proxy` to decide whether to route traffic.

## 11.5 Optional Prometheus + Grafana Compose Addon

```
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
```

Adds `iams-prometheus` (scrape target: `iams-backend:8080/actuator/prometheus`) and `iams-grafana` (pre-provisioned dashboard JSON checked into the repo, covering every metric in 11.3), both on `iams-internal` only — not externally exposed by default. Entirely optional; documented in the Installation Guide as an addon for organizations with the appetite to run it, never required for go-live (BRD 11.2).

---

# 12. Concurrency & Connection-Pool Implications (NFR-CONC-02)

Inventory quantity mutations (FR-INV-02, FR-INV-05, FR-INV-08) use atomic, row-level operations — `UPDATE ... SET quantity = quantity - :n WHERE quantity >= :n` or a short `SELECT ... FOR UPDATE` transaction — rather than optimistic locking, precisely because they're high-contention (SRS NFR-CONC-02). This has two direct infrastructure consequences reflected elsewhere in this document:

1. **Transactions touching inventory quantity must be kept as short as possible** — no external calls (notification dispatch, Integration Gateway calls, report generation) inside the same transaction as a quantity mutation. Stock Transfer's decrement-source/increment-destination pair (FR-INV-08, UC-INV-01) is the canonical example: both operations happen inside one short transaction, and anything else (notifying the warehouse manager, logging the linked source/destination pair) happens **after** commit, asynchronously.
2. **HikariCP pool sizing (Section 7.3)** is set with headroom above the expected concurrent-mutation rate so a burst of Stock In/Out/Transfer/Adjustment activity during, e.g., event setup doesn't exhaust the pool and start queuing unrelated read requests — this is why the large-deployment sizing guidance (Section 7.3) recommends 30–50 connections rather than scaling `max_connections` linearly with user count alone.

SoD checks (Section 3) that gate FR-INV-05 Adjustments (mandatory reason **and approver** — the approver must not be the requester) run as part of the same short transaction as the adjustment itself, not as a separate round-trip, to avoid a check-then-act race between authorization and the mutation it's gating.

---

# 13. Incident Response Runbook Skeleton

Ties directly to FR-SEC-04's immutable security-event log — this is the runbook Officer Reyes (IT Security Officer, IAMS-PUC-1.1) actually uses, following the exact investigation pattern in UC-SEC-01.

## 13.1 Roles

| Role | Responsibility |
|---|---|
| IT Security Officer | Incident commander; classifies severity; owns the investigation and technical containment decisions (BRD Stakeholder table, Section 2) |
| Super Administrator | Executes technical containment actions (revoke sessions, disable accounts, disable an integration, rotate a credential) |
| Data Protection / Compliance Officer | Assesses legal notification obligations and timeline; determines whether personal data was exposed |
| Executive Sponsor | Final risk-acceptance / external-communication decision when the IT Security Officer or Compliance Officer's objection needs escalation (BRD 14.1) |

## 13.2 Phases

1. **Detection** — triggered by a SEC log anomaly (repeated failed logins, unusual admin-action pattern — the exact UC-SEC-01 pattern), a Prometheus alert (e.g., a spike in `iams_auth_login_attempts_total{outcome="failure"}` or `iams_authz_denials_total`), a user report, or a vulnerability-scan finding (FR-SEC-13).
2. **Triage & Classification** — IT Security Officer classifies severity (Critical/High/Medium/Low) and type (unauthorized access, data exposure, malware, denial of service, integration compromise) within a target window (placeholder: 1 hour for Critical — confirm against the organization's own support SLA during Phase 0 discovery, BRD 8.3).
3. **Containment** — concrete, available actions: force-revoke all refresh tokens for an account/org (bumps `security_stamp`, Section 2.1 — takes effect within the local cache TTL); disable the account; add an IP block via the reverse-proxy allow/deny list (FR-SEC-07); disable an affected integration adapter (`enabled=false`, effective immediately per Section 8.1's gate); isolate the host from the network if the compromise is server-level; rotate the specific credential class implicated (Section 5.4's table).
4. **Investigation** — pull the immutable SEC log (FR-SEC-04) filtered by user/date/IP exactly as in UC-SEC-01; correlate across services via `correlationId` (Section 11.2); determine scope of exposure (which `orgNodeId`/entity records were touched).
5. **Eradication & Recovery** — patch/redeploy the fix through the CI/CD pipeline (Section 9); if data integrity is compromised, restore from a verified backup via `restore.sh` (Section 10.3); re-enable affected accounts/integrations only after root cause is addressed.
6. **Notification** — the Data Protection/Compliance Officer determines the applicable notification obligation and timeline. *Placeholder, to be filled in per deployment's applicable law during Phase 0 discovery (BRD 6.1, 11.1):* e.g., a GDPR-style regime's 72-hour regulator-notification clock, or the specific U.S. state breach-notification statute applicable to the organization's jurisdiction — this varies by deployment and is intentionally not hardcoded here. An unresolved objection from either the IT Security Officer or the Compliance Officer escalates to the Executive Sponsor per BRD 14.1, never silently overridden by schedule pressure.
7. **Post-Incident Review** — a root-cause document; identified control gaps tracked to closure (mirroring the FR-SEC-13 penetration-test-finding tracking pattern); the incident record itself, and every containment action taken, is retained in the immutable log — and placed under legal hold (FR-CMP-06) if litigation or regulatory inquiry is reasonably anticipated.

Every containment action in Phase 3 is itself an administrative action logged immutably (FR-SEC-04) — the incident-response process generates its own audit trail rather than operating outside the system's own logging, which is exactly what Officer Reyes's persona goal ("I need evidence, not assurances — I want to see the log") requires.

---

# Appendix A: Requirement Traceability Matrix

| Section | Primary FR/NFR IDs |
|---|---|
| 1. Filter Chain | NFR-SEC-01/02, NFR-SEC-10 (flagged, Section 1), NFR-OBS-03, NFR-MAINT-04, NFR-API-01/02, FR-SEC-01, FR-USR-06/09, FR-AUD-22 |
| 2. AuthN | FR-SEC-01, FR-SEC-02, FR-SEC-03, FR-SEC-05, NFR-SEC-01, SRS 6.1, SRS 6.6 (LDAP/SSO rows) |
| 3. AuthZ | NFR-SEC-02, FR-USR-03/04/06/09, FR-AUD-22, FR-SEC-06, FR-SEC-16 (flagged, Section 3.5), SRS 6.2 |
| 4. CORS/CSRF | SRS 6.7, OWASP ASVS V4 |
| 5. Secrets | FR-SEC-15, NFR-DEPLOY-03, SRS 6.5 |
| 6. Rate Limiting | NFR-API-01, NFR-API-02 |
| 7. Docker Compose Topology | SRS 2.5, SRS 2.6, NFR-DEPLOY-01/02/03, SRS 6.3, SRS 6.8, NFR-SCALE-01/02 |
| 8. Integration Gateway | FR-SEC-09, FR-INT-06, FR-SEC-14, NFR-SEC-11, FR-INT-01–05, SRS 6.6, BRD 6.4/6.5 |
| 9. CI/CD | FR-SEC-12, NFR-MAINT-02/03/05, SRS 6.8 |
| 10. Backup/Restore/DR | NFR-AVAIL-02/03/04, BRD Section 10 |
| 11. Observability | NFR-OBS-01/02/03, SRS Section 3 (Observability row) |
| 12. Concurrency | NFR-CONC-01/02 |
| 13. Incident Response | FR-SEC-04, FR-SEC-13, BRD Section 10, BRD 14.1, UC-SEC-01 |

# Appendix B: Judgment Calls Made in This Document (Log for Later Re-Review)

| # | Decision | Where | Why it was left open by prior docs |
|---|---|---|---|
| 1 | RS256 (asymmetric) over HS256 for JWT signing | Section 2.1 | SRS specifies "JWT tokens" without an algorithm |
| 2 | Security-stamp claim + 60s local cache for near-immediate revocation, instead of a full access-token denylist | Section 2.1 | NFR-SEC-01 requires revocability but not a specific mechanism |
| 3 | Password expiry defaults OFF (NIST 800-63B alignment), despite FR-SEC-05 requiring the *capability* to configure it | Section 2.5 | FR-SEC-05 requires configurability, not a default value |
| 4 | Argon2id as default password hash, bcrypt as documented fallback for constrained hosts | Section 2.5 | NFR-SEC-01 permits either |
| 5 | Centralized `AccessPolicyEvaluator` invoked via `@PreAuthorize`, rather than pure scattered annotations or pure imperative checks | Section 3.2 | SRS states RBAC+scope compose but not the implementation pattern; SoD's introduction as a third layer created a new composition problem prior docs didn't resolve |
| 6 | CSRF protection disabled for bearer-token endpoints, enabled only for the two cookie-authenticated auth endpoints | Section 4 | SRS 6.7 requires "CSRF protection on state-changing requests" without resolving the bearer-vs-cookie architecture question it explicitly flagged as needing clarification |
| 7 | SOPS-encrypted `.env` + Compose file secrets as the primary secrets mechanism; Vault as an explicit opt-in, never mandatory | Section 5 | SRS 6.5 asks for "a secrets manager or encrypted environment configuration" without naming one; task explicitly asked for a pragmatic default to be proposed |
| 8 | Two-tier rate limiting (Nginx per-IP coarse, Bucket4j in-memory per-user/per-key fine) rather than a single-tier or Redis-backed design | Section 6 | NFR-API-01 specifies "at the reverse-proxy/API-gateway layer" without addressing that per-user/per-key limiting needs identity the proxy doesn't cheaply have |
| 9 | Two Docker networks (`iams-edge`, `iams-internal` with `internal: true`) as the concrete segmentation mechanism | Section 7.1 | SRS 6.8 states the *requirement* (DB/object-store not externally reachable) without specifying the Compose-level mechanism |
| 10 | `IAMS_SERVE_FRONTEND` toggle as the concrete small-vs-large deployment switch | Section 7.5 | SRS 2.5 offers both frontend-serving modes as valid without specifying how a deployment chooses between them |
| 11 | GitHub Actions (or a self-hosted equivalent) assumed as the CI runner, with all scan tooling chosen to be free/self-hostable (Semgrep OSS, OWASP Dependency-Check, Trivy) | Section 9 | Task explicitly required this assumption to be stated rather than assuming a specific paid vendor |
| 12 | Rollback strategy is "redeploy prior image against forward-compatible schema," not a Flyway schema-level undo | Section 9.2 | NFR-MAINT-05 requires "a documented rollback procedure" without specifying the mechanism, and Flyway Community Edition has no built-in undo |
| 13 | Quarterly restore-test automated against Staging (not Production), triggered by a scheduled job rather than solely a human calendar reminder | Section 10.4 | NFR-AVAIL-04 requires verification "at least quarterly" without mandating automation — chosen because a manual-only process is exactly the kind of check a part-time IT generalist (BRD 11.2) is most likely to let lapse |
| 14 | Notification-timeline placeholder left jurisdiction-specific rather than hardcoded to one regime (e.g., GDPR's 72 hours) | Section 13.2 | BRD/SRS reference GDPR/FERPA as reference models "whether or not it applies directly," consistent with the sector/geography-agnostic design principle (BRD 1.1) |

---

**End of Document.**
