package com.iams.common.security;

import java.util.UUID;

/**
 * US-SEC-06: enforces an idle-session timeout on the otherwise-stateless access
 * token. Access tokens are JWTs valid for their whole absolute lifetime; on its
 * own that lets an unattended device keep a live session until the token
 * naturally expires. This guard makes the JWT filter refuse a token whose user
 * has made no request within the configured idle window, forcing re-auth.
 * <p>
 * The port lives in common.security (like {@link CurrentUserProvider} and
 * {@link AccessRevocationCheck}); the adapter that tracks last-seen activity
 * lives in the sec module with the security-event log it writes to.
 */
public interface SessionActivityGuard {

    /** Begin (or restart) a user's idle clock — called when a fresh access token is issued at login/refresh. */
    void start(UUID userId);

    /**
     * Per-request: records activity and returns {@code true} if the session is
     * still within the idle window; returns {@code false} if it has idled past
     * the timeout, in which case the caller must refuse the request (401).
     */
    boolean recordActivity(UUID userId);
}
