package com.iams.sec.application;

import com.iams.common.security.DevSecurityProperties;
import com.iams.common.security.SessionActivityGuard;
import com.iams.sec.domain.SecurityEventType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * US-SEC-06: the in-memory sliding-window idle tracker behind
 * {@link SessionActivityGuard}. One last-seen timestamp per user, updated on
 * every request; a gap larger than the configured idle window means the next
 * request is refused (the JWT filter leaves the SecurityContext empty, so the
 * normal 401 entry point responds) and a SESSION_EXPIRED event is logged once.
 * <p>
 * On expiry the entry is parked at a sentinel rather than removed, so repeated
 * retries of the same stale token keep being refused (and don't re-log) until a
 * genuine re-login via {@link #start} resets the clock. Design mirrors the
 * US-USR-08 DeactivatedUserRegistry: O(1) per request, no database hit on the
 * auth hot path.
 * <p>
 * Known limitations, stated plainly: (1) per-instance, like every deployment-
 * local choice in this app — a horizontally-scaled deployment would need a
 * shared store; (2) the map is not seeded at startup (last-activity isn't
 * persisted), so immediately after a restart a token that had already idled out
 * gets one fresh window — bounded by the access token's own absolute expiry and
 * by the refresh token's separate idle timeout (US-SEC-01), both still enforced.
 */
@Component
public class SessionActivityRegistry implements SessionActivityGuard {

    /** Parked here after an idle expiry so retries keep failing without re-logging until re-login. */
    private static final Instant EXPIRED = Instant.MIN;

    private final Map<UUID, Instant> lastSeen = new ConcurrentHashMap<>();
    private final Duration idleTimeout;
    private final SecurityEventLogger securityEventLogger;
    private volatile Clock clock = Clock.systemUTC();

    public SessionActivityRegistry(DevSecurityProperties properties, SecurityEventLogger securityEventLogger) {
        this.idleTimeout = properties.getSession().getIdleTimeout();
        this.securityEventLogger = securityEventLogger;
    }

    /** Test seam only: lets a test advance time deterministically instead of sleeping. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void start(UUID userId) {
        lastSeen.put(userId, clock.instant());
    }

    @Override
    public boolean recordActivity(UUID userId) {
        Instant now = clock.instant();
        // Transition the last-seen state atomically so a user's concurrent in-flight
        // requests (e.g. a dashboard firing several parallel XHRs) can't each observe
        // the pre-expiry timestamp and each park+log the expiry - exactly one caller
        // wins the ACTIVE->EXPIRED move, so SESSION_EXPIRED is logged once. The lambda
        // stays side-effect-free (no I/O under the bin lock); the log write happens
        // after compute returns, keyed off whether this call caused the transition.
        boolean[] justExpired = {false};
        Instant updated = lastSeen.compute(userId, (id, previous) -> {
            if (EXPIRED.equals(previous)) {
                return EXPIRED; // already idled out and logged; awaiting a fresh login
            }
            if (previous != null && Duration.between(previous, now).compareTo(idleTimeout) > 0) {
                justExpired[0] = true;
                return EXPIRED;
            }
            return now; // fresh (never seen) or within the window: slide the clock forward
        });

        if (justExpired[0]) {
            securityEventLogger.record(SecurityEventType.SESSION_EXPIRED, userId, null, null, null);
            return false;
        }
        return !EXPIRED.equals(updated);
    }
}
