package com.iams.sec.domain;

/**
 * US-SEC-04's AC names "every login, permission change, export, and audit
 * submission" - EXPORT/AUDIT_SUBMISSION have no epic behind them yet
 * (EPIC-CMP/EPIC-AUD are both unstarted), so only the event types that
 * actually occur in the codebase today are listed. Add to this enum, don't
 * invent a generic "OTHER", as each new event-producing feature lands.
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
    SESSION_EXPIRED
}
