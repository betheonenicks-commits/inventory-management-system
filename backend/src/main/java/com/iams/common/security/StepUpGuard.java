package com.iams.common.security;

import java.util.UUID;

/**
 * US-SEC-06 (AC-SEC-06-X): a step-up-required action (currently, editing what
 * a role grants) demands a recent password re-entry, separate from the
 * session's own idle-timeout tracked by SessionActivityGuard. Mirrors that
 * guard's shape deliberately - same in-memory, per-userId freshness idea,
 * different clock.
 */
public interface StepUpGuard {

    /** Called once the caller has just re-entered their password successfully. */
    void confirm(UUID userId);

    /** Throws StepUpRequiredException if this user has no still-valid step-up confirmation. */
    void requireVerified(UUID userId);
}
