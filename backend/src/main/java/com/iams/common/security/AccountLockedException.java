package com.iams.common.security;

import java.time.Instant;

/**
 * US-SEC-09 exception AC: a locked account gets a distinct message even with
 * correct credentials - deliberately not folded into InvalidCredentialsException's
 * generic "invalid username or password", since the AC explicitly wants the
 * lockout state surfaced, unlike username existence (AC-SEC-04-X).
 */
public class AccountLockedException extends RuntimeException {

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("Account locked due to repeated failed login attempts. Try again later.");
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
