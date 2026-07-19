package com.iams.sec.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.iams.common.security.DevSecurityProperties;
import com.iams.sec.domain.SecurityEventType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionActivityRegistryTest {

    @Mock private SecurityEventLogger securityEventLogger;

    private SessionActivityRegistry registry;
    private MutableClock clock;
    private final UUID user = UUID.randomUUID();

    /** A clock the test advances by hand, so idle behavior is deterministic (no sleeps). */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-07-19T10:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public Instant instant() { return now; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    @BeforeEach
    void setUp() {
        DevSecurityProperties props = new DevSecurityProperties();
        props.getSession().setIdleTimeout(Duration.ofMinutes(30));
        registry = new SessionActivityRegistry(props, securityEventLogger);
        clock = new MutableClock();
        registry.setClock(clock);
    }

    @Test
    void activityWithinWindowKeepsTheSessionAlive() {
        registry.start(user);
        clock.advance(Duration.ofMinutes(20));
        assertThat(registry.recordActivity(user)).isTrue();  // 20 min < 30
        clock.advance(Duration.ofMinutes(25));               // 25 min since the last activity, not since login
        assertThat(registry.recordActivity(user)).isTrue();  // sliding window: still alive
        verifyNoInteractions(securityEventLogger);
    }

    @Test
    void idlePastTheWindowIsRefusedAndLoggedOnce() {
        registry.start(user);
        clock.advance(Duration.ofMinutes(31));               // idled past 30 min
        assertThat(registry.recordActivity(user)).isFalse();
        // Retries of the same stale token keep failing but must not re-log.
        assertThat(registry.recordActivity(user)).isFalse();
        assertThat(registry.recordActivity(user)).isFalse();
        verify(securityEventLogger, times(1)).record(SecurityEventType.SESSION_EXPIRED, user, null, null, null);
    }

    @Test
    void reLoginAfterIdleExpiryRestartsTheClock() {
        registry.start(user);
        clock.advance(Duration.ofMinutes(31));
        assertThat(registry.recordActivity(user)).isFalse(); // expired

        registry.start(user);                                // fresh login resets
        assertThat(registry.recordActivity(user)).isTrue();  // alive again
    }

    @Test
    void firstRequestForAnUnknownUserStartsTheClock_notImmediatelyExpired() {
        // Post-restart / never-seen: no entry yet -> treated as fresh, not idle.
        assertThat(registry.recordActivity(user)).isTrue();
        verifyNoInteractions(securityEventLogger);
    }

    @Test
    void concurrentRequestsCrossingTheWindowExpireOnceAndLogOnce() throws Exception {
        // A user's parallel in-flight requests all hit recordActivity() just after the
        // idle window elapsed. Every one must be refused, and SESSION_EXPIRED must be
        // logged exactly once - not once per racing request (the get-check-put bug).
        registry.start(user);
        clock.advance(Duration.ofMinutes(31));

        int threads = 16;
        var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var startLine = new java.util.concurrent.CyclicBarrier(threads);
        var results = new java.util.concurrent.CopyOnWriteArrayList<Boolean>();
        var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    startLine.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                results.add(registry.recordActivity(user));
            }));
        }
        for (var f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(results).hasSize(threads).containsOnly(false); // every racing request refused
        verify(securityEventLogger, times(1)).record(SecurityEventType.SESSION_EXPIRED, user, null, null, null);
    }
}
