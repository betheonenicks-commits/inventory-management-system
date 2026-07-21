package com.iams.sec.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iams.common.security.DevSecurityProperties;
import com.iams.common.security.StepUpRequiredException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StepUpRegistryTest {

    private StepUpRegistry registry;
    private MutableClock clock;
    private final UUID user = UUID.randomUUID();

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
        props.getStepUp().setValidity(Duration.ofMinutes(5));
        registry = new StepUpRegistry(props);
        clock = new MutableClock();
        registry.setClock(clock);
    }

    @Test
    void requireVerified_throws_whenNeverConfirmed() {
        assertThatThrownBy(() -> registry.requireVerified(user)).isInstanceOf(StepUpRequiredException.class);
    }

    @Test
    void requireVerified_passes_immediatelyAfterConfirm() {
        registry.confirm(user);
        assertThatCode(() -> registry.requireVerified(user)).doesNotThrowAnyException();
    }

    @Test
    void requireVerified_passes_withinTheValidityWindow() {
        registry.confirm(user);
        clock.advance(Duration.ofMinutes(4));
        assertThatCode(() -> registry.requireVerified(user)).doesNotThrowAnyException();
    }

    @Test
    void requireVerified_throws_oncePastTheValidityWindow() {
        registry.confirm(user);
        clock.advance(Duration.ofMinutes(6));
        assertThatThrownBy(() -> registry.requireVerified(user)).isInstanceOf(StepUpRequiredException.class);
    }

    @Test
    void reConfirmingRestartsTheWindow() {
        registry.confirm(user);
        clock.advance(Duration.ofMinutes(6));
        assertThatThrownBy(() -> registry.requireVerified(user)).isInstanceOf(StepUpRequiredException.class);

        registry.confirm(user);
        assertThatCode(() -> registry.requireVerified(user)).doesNotThrowAnyException();
    }

    @Test
    void confirmationIsPerUser() {
        UUID other = UUID.randomUUID();
        registry.confirm(user);
        assertThatThrownBy(() -> registry.requireVerified(other)).isInstanceOf(StepUpRequiredException.class);
    }
}
