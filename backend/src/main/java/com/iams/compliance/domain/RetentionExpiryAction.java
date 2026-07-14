package com.iams.compliance.domain;

/** US-CMP-01: what happens to a row once it's older than its entity type's retention period. */
public enum RetentionExpiryAction {
    DELETE,
    ANONYMIZE,
    HOLD_ELIGIBLE
}
