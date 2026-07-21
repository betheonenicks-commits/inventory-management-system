package com.iams.common.security;

import java.util.Optional;

/**
 * US-SEC-14: port that resolves a raw API key to an authenticated service-account
 * principal, consulted by {@link ServiceAccountAuthenticationFilter} on the auth hot
 * path. Port lives in common.security (like {@link AccessRevocationCheck}); the adapter
 * that hashes the key and reads the service_account table lives in the sec module.
 */
public interface ServiceAccountAuthenticator {

    /** Empty if the key is unknown, malformed, or belongs to a deactivated account. */
    Optional<ServiceAccountPrincipal> authenticate(String rawApiKey);
}
