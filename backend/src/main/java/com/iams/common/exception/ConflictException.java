package com.iams.common.exception;

import java.util.Map;

/**
 * Generic 409 for state conflicts that aren't optimistic-locking related,
 * e.g. deleting a category still referenced by assets. errorCode is
 * scenario-specific (per the API spec's error catalog, e.g.
 * ORG_NODE_HAS_DEPENDENTS, USER_HAS_OUTSTANDING_ASSIGNMENTS - each conflict
 * scenario gets its own code, not one shared generic code); title and
 * extraProperties are optional overrides for callers whose API-spec contract
 * documents a specific title and machine-readable payload (e.g. a
 * blockingAssets list) beyond what a generic detail string can express.
 */
public class ConflictException extends RuntimeException {

    private final String errorCode;
    private final String title;
    private final Map<String, Object> extraProperties;

    public ConflictException(String errorCode, String message) {
        this(errorCode, "Conflict", message, Map.of());
    }

    public ConflictException(String errorCode, String title, String message, Map<String, Object> extraProperties) {
        super(message);
        this.errorCode = errorCode;
        this.title = title;
        this.extraProperties = extraProperties;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, Object> getExtraProperties() {
        return extraProperties;
    }
}
