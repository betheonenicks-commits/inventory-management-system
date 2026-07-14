package com.iams.common.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * US-USR-03: @PreAuthorize checks against the acting user's computed
 * permission set (role.permissions, unioned across their flat role
 * assignments - see CurrentUser.permissions/AuthController), not a hardcoded
 * role-name literal. This is what lets a custom role (US-USR-02) actually
 * grant capability through the API, rather than only through roles the
 * original hasRole/hasAnyRole checks happened to name.
 */
@Component("perm")
public class PermissionChecker {

    public boolean has(String permission) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof CurrentUser currentUser && currentUser.hasPermission(permission);
    }
}
