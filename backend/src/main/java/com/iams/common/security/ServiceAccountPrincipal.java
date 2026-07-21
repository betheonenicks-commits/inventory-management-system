package com.iams.common.security;

import java.util.Set;
import java.util.UUID;

/**
 * US-SEC-14: the SecurityContext principal for an authenticated service account.
 * Deliberately NOT a {@link CurrentUser} - so every {@code @perm.has(...)} check
 * (which only recognises CurrentUser) returns false for a service account, i.e. it
 * is refused every human endpoint by default. Its only capability is {@link #scopes},
 * checked by {@code @svc.hasScope(...)} on the integration endpoints opened to it.
 */
public record ServiceAccountPrincipal(UUID id, String name, Set<String> scopes) {

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
}
