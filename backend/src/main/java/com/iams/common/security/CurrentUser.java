package com.iams.common.security;

import java.util.Set;
import java.util.UUID;

/**
 * The authenticated principal attached to the SecurityContext by JwtAuthenticationFilter.
 * Mirrors the shape GET /auth/me returns per the API spec.
 */
public record CurrentUser(UUID id, String username, Set<String> roles, Set<String> permissions) {

    /** Existing callers (mostly tests) that don't care about permission-based checks. */
    public CurrentUser(UUID id, String username, Set<String> roles) {
        this(id, username, roles, Set.of());
    }

    public boolean hasPermission(String permission) {
        return permissions.contains("*") || permissions.contains(permission);
    }
}
