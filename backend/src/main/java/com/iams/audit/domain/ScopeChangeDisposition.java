package com.iams.audit.domain;

/** US-AUD-23: how a SCOPE_CHANGED finding is ultimately resolved before closure is allowed. */
public enum ScopeChangeDisposition {
    CONFIRM_VERIFIED_AT_NEW_LOCATION,
    EXCLUDE_FROM_SCOPE,
    ACCEPT_AS_EXCEPTION
}
