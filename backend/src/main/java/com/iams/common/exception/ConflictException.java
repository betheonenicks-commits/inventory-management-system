package com.iams.common.exception;

/**
 * Generic 409 for state conflicts that aren't optimistic-locking related,
 * e.g. deleting a category still referenced by assets (errorCode ORG_NODE_HAS_DEPENDENTS,
 * reused per the API spec's error catalog for any dependent-blocked deletion).
 */
public class ConflictException extends RuntimeException {

    private final String errorCode;

    public ConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
