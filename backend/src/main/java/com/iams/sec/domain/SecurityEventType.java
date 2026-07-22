package com.iams.sec.domain;

/**
 * US-SEC-04's AC names "every login, permission change, export, and audit
 * submission" - all four are now covered (REPORT_EXPORTED / AUDIT_SUBMITTED
 * closed the last two, added once EPIC-RPT/EPIC-AUD existed to produce
 * them). Add to this enum, don't invent a generic "OTHER", as each new
 * event-producing feature lands.
 */
public enum SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    PERMISSION_DENIED,
    ROLE_ASSIGNED,
    USER_DEACTIVATED,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    LOGOUT,
    LOGOUT_ALL,
    /** A refresh token already revoked (rotated or logged out) was presented again - possible token theft/replay. */
    REFRESH_TOKEN_REUSE_DETECTED,
    /** US-SEC-06: a refresh token was refused for having gone unused longer than the configured idle timeout. */
    SESSION_EXPIRED,
    /** US-CMP-01: "the purge itself is logged" - one row per retention-purge run, naming what was purged and how many rows. */
    RETENTION_PURGE_EXECUTED,
    /** US-CMP-02: a departed person's personal data was anonymized following Compliance Officer approval. */
    PERSON_ANONYMIZED,
    /** US-SEC-14: a scoped integration service account was issued (its raw API key returned once). */
    SERVICE_ACCOUNT_CREATED,
    /** US-SEC-14: a service account was revoked - its API key no longer authenticates. */
    SERVICE_ACCOUNT_REVOKED,
    /** US-SEC-15 / FR-INT-05: an integration was registered (credentials held only as a secrets-manager reference). */
    INTEGRATION_CREATED,
    /** FR-INT-05: an integration was enabled (an outbound/data flow turned on). */
    INTEGRATION_ENABLED,
    /** FR-INT-05: an integration was disabled. */
    INTEGRATION_DISABLED,
    /** US-SEC-15 / FR-INT-05: an integration registration was deleted. */
    INTEGRATION_DELETED,
    /** US-SEC-04: a report was actually exported to a file (CSV/XLSX/PDF, sync or background) - not just viewed as JSON. */
    REPORT_EXPORTED,
    /** US-SEC-04: an audit was signed and submitted for approval. */
    AUDIT_SUBMITTED,
    /** US-SEC-09: a locked-out user requested a self-service unlock email. */
    ACCOUNT_UNLOCK_REQUESTED,
    /** US-SEC-06: a step-up-required action's password re-confirmation succeeded. */
    STEP_UP_VERIFIED,
    /** US-MIG-03: a bulk import run was committed - names the run, entity type, and its created/failed/skipped reconciliation. */
    BULK_IMPORT_COMMITTED
}
