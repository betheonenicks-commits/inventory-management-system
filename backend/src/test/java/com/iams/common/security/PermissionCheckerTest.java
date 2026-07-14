package com.iams.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PermissionCheckerTest {

    private final PermissionChecker checker = new PermissionChecker();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantsWhenPermissionPresent() {
        authenticateAs(Set.of("assets:write"));
        assertThat(checker.has("assets:write")).isTrue();
    }

    @Test
    void deniesWhenPermissionAbsent() {
        authenticateAs(Set.of("assets:read"));
        assertThat(checker.has("assets:write")).isFalse();
    }

    @Test
    void wildcardGrantsEverything() {
        authenticateAs(Set.of("*"));
        assertThat(checker.has("anything:at-all")).isTrue();
    }

    private void authenticateAs(Set<String> permissions) {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), "tester", Set.of(), permissions);
        var token = new UsernamePasswordAuthenticationToken(user, null, Set.of());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
