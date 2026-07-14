package com.iams.common.security.api;

import java.util.Set;
import java.util.UUID;

/**
 * GET /auth/me shape (US-USR-01 AC): role(s), org scope, and the effective
 * permission set computed as the union of every assigned role's explicit
 * permissions (US-USR-07: flat, non-inheriting - nothing implied beyond that
 * union).
 */
public record MeResponse(
        UUID id,
        String username,
        String displayName,
        Set<String> roles,
        UUID orgScopeNodeId,
        Set<String> permissions
) {
}
