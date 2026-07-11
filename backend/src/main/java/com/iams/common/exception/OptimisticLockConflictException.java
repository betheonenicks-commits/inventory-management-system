package com.iams.common.exception;

/**
 * Raised when a PATCH carries a stale `version`. Carries enough state for the
 * client to reconcile: the version it thought was current, the version that
 * actually is current, and the full current resource (per API spec Section 5.1).
 */
public class OptimisticLockConflictException extends RuntimeException {

    private final long expectedVersion;
    private final long currentVersion;
    private final Object currentResource;

    public OptimisticLockConflictException(long expectedVersion, long currentVersion, Object currentResource) {
        super("Optimistic lock conflict: expected version " + expectedVersion + " but current is " + currentVersion);
        this.expectedVersion = expectedVersion;
        this.currentVersion = currentVersion;
        this.currentResource = currentResource;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public long getCurrentVersion() {
        return currentVersion;
    }

    public Object getCurrentResource() {
        return currentResource;
    }
}
