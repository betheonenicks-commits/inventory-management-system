package com.iams.sec.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * US-SEC-14: the fixed catalogue of integration scopes a service account may hold.
 * A scope is the ONLY thing a service account can do - it maps to specific
 * integration endpoints (opened via {@code @svc.hasScope('...')}), never to the
 * broad human permission set. Keeping this a closed enum means an unknown scope is
 * rejected at creation rather than silently granting nothing.
 */
public enum IntegrationScope {

    /** Read-only access to accounting-relevant exports (e.g. the depreciation report). */
    INT_ACCOUNTING_READ;

    public static boolean isValid(String scope) {
        return Arrays.stream(values()).anyMatch(s -> s.name().equals(scope));
    }

    public static Set<String> names() {
        return Arrays.stream(values()).map(s -> s.name()).collect(Collectors.toSet());
    }
}
