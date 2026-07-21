package com.iams.common.security;

import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class DevCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser current() {
        return currentOrEmpty().orElseThrow(() -> new AccessDeniedException("No authenticated user in context"));
    }

    @Override
    public Optional<CurrentUser> currentOrEmpty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CurrentUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }
}
