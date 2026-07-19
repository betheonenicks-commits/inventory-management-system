package com.iams.common.security;

import java.util.UUID;

/**
 * US-USR-08: a seam the JWT filter consults on every authenticated request to
 * decide whether a still-unexpired access token must nonetheless be refused -
 * because the user it names has since been deactivated. Access tokens are
 * stateless JWTs, so without this check a deactivated user would keep working
 * until the token's natural expiry (up to the configured lifetime). The port
 * lives in common.security (like {@link CurrentUserProvider}); the adapter that
 * actually tracks deactivations lives in the usr module with the user data.
 */
public interface AccessRevocationCheck {

    /** True if access tokens for this user must be refused regardless of their expiry. */
    boolean isRevoked(UUID userId);
}
