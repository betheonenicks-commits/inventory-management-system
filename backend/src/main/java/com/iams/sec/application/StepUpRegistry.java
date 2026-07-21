package com.iams.sec.application;

import com.iams.common.security.DevSecurityProperties;
import com.iams.common.security.StepUpGuard;
import com.iams.common.security.StepUpRequiredException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * US-SEC-06 (AC-SEC-06-X). Same shape as SessionActivityRegistry: an
 * in-memory, per-userId last-confirmed-at map, no DB row - a step-up
 * confirmation is a short-lived fact about the live session, not
 * history worth persisting.
 */
@Component
public class StepUpRegistry implements StepUpGuard {

    private final Map<UUID, Instant> verifiedAt = new ConcurrentHashMap<>();
    private final Duration validity;
    private volatile Clock clock = Clock.systemUTC();

    public StepUpRegistry(DevSecurityProperties properties) {
        this.validity = properties.getStepUp().getValidity();
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void confirm(UUID userId) {
        verifiedAt.put(userId, clock.instant());
    }

    @Override
    public void requireVerified(UUID userId) {
        Instant last = verifiedAt.get(userId);
        if (last == null || Duration.between(last, clock.instant()).compareTo(validity) > 0) {
            throw new StepUpRequiredException();
        }
    }
}
