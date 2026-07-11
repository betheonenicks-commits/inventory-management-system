package com.iams.common.security;

/**
 * Seam for "who is making this request" - used by application services to
 * populate created_by/updated_by. In this dev-stub phase there is exactly one
 * possible user; EPIC-USR/SEC will replace the implementation, not this interface.
 */
public interface CurrentUserProvider {

    CurrentUser current();
}
