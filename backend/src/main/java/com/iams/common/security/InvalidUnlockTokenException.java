package com.iams.common.security;

/**
 * US-SEC-09: a self-service unlock confirmation refused - unknown, already
 * used, or expired. One message for all three, matching InvalidRefreshTokenException's
 * reasoning: which specific reason it failed is for the Security & Access Log, not
 * the response body.
 */
public class InvalidUnlockTokenException extends RuntimeException {

    public InvalidUnlockTokenException() {
        super("Invalid or expired unlock code");
    }
}
