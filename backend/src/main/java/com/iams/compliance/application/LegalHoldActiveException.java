package com.iams.compliance.application;

import com.iams.compliance.domain.LegalHoldScopeType;

/**
 * AC-CMP-06-H: "retention purge or anonymization... blocked (423) until the
 * hold is lifted" - a distinct HTTP status from a plain 409 conflict, same
 * reasoning AccountLockedException already established for US-SEC-09's
 * lockout state (a real, different condition from a generic state conflict,
 * not folded into ConflictException).
 */
public class LegalHoldActiveException extends RuntimeException {

    private final LegalHoldScopeType scopeType;

    public LegalHoldActiveException(LegalHoldScopeType scopeType) {
        super("This " + scopeType + " is under an active legal hold and cannot be purged, disposed, or anonymized until it is lifted");
        this.scopeType = scopeType;
    }

    public LegalHoldScopeType getScopeType() {
        return scopeType;
    }
}
