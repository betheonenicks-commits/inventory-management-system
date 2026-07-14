package com.iams.audit.domain;

/** US-AUD-05/09/23: how a single expected asset resolved within an audit. */
public enum FindingStatus {
    VERIFIED,
    MISSING,
    OUT_OF_SCOPE,
    SCOPE_CHANGED
}
