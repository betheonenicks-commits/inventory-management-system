package com.iams.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * US-SEC-14: {@code @svc.hasScope('INT_ACCOUNTING_READ')} for @PreAuthorize - the
 * service-account counterpart of {@link PermissionChecker} ({@code @perm}). Only an
 * authenticated {@link ServiceAccountPrincipal} can satisfy it; a human (CurrentUser)
 * never does, and a service account without the scope never does. Integration
 * endpoints OR this with the human permission, e.g.
 * {@code @perm.has('reports:read') or @svc.hasScope('INT_ACCOUNTING_READ')}.
 */
@Component("svc")
public class ServiceAccountScopeChecker {

    public boolean hasScope(String scope) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getPrincipal() instanceof ServiceAccountPrincipal principal
                && principal.hasScope(scope);
    }
}
