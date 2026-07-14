package com.iams.compliance.domain;

/**
 * US-CMP-01: the closed set of entity types a Compliance Officer can attach a
 * retention policy to. BRD §5.4's named floors apply to SECURITY_EVENT_LOG
 * (7 years) and DISPOSED_ASSET (3 years) - {@link RetentionPolicyService}
 * enforces those two; the others have no fixed floor ("personal data:
 * retained per configured policy").
 */
public enum RetentionEntityType {
    SECURITY_EVENT_LOG,
    DISPOSED_ASSET,
    PERSON,
    ASSET_HISTORY_EVENT,
    AUDIT_RECORD
}
