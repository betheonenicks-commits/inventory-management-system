package com.iams.common.security;

/**
 * US-SEC-01: a refresh-token exchange refused - unknown, expired, already
 * revoked (rotation already consumed it, or logout/logout-all revoked it),
 * or idle too long (US-SEC-06). Deliberately one message for all of these,
 * the same reasoning InvalidCredentialsException uses for login - which
 * specific reason it failed is for the Security & Access Log, not the
 * response body.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
