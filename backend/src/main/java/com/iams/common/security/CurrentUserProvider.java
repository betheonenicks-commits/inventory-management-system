package com.iams.common.security;

/**
 * Seam for "who is making this request" - used by application services to
 * populate created_by/updated_by. In this dev-stub phase there is exactly one
 * possible user; EPIC-USR/SEC will replace the implementation, not this interface.
 */
import java.util.Optional;

public interface CurrentUserProvider {

    CurrentUser current();

    /**
     * The current human user if there is one, else empty - unlike {@link #current()},
     * this never throws when the caller is a non-human principal (US-SEC-14 service
     * account) or unauthenticated. Lets org-scope logic treat a service account as
     * unrestricted rather than blowing up on the CurrentUser cast.
     */
    Optional<CurrentUser> currentOrEmpty();
}
