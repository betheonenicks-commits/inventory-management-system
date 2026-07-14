package com.iams.usr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserLockoutServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private SecurityEventLogger securityEventLogger;

    private UserLockoutService service;

    @BeforeEach
    void setUp() {
        service = new UserLockoutService(userRepository, securityEventLogger);
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername("tester");
        return user;
    }

    @Test
    void isLocked_false_whenLockedUntilIsNull() {
        assertThat(service.isLocked(user())).isFalse();
    }

    @Test
    void isLocked_true_whenLockedUntilIsInTheFuture() {
        AppUser user = user();
        user.setLockedUntil(Instant.now().plusSeconds(60));
        assertThat(service.isLocked(user)).isTrue();
    }

    @Test
    void isLocked_false_whenLockedUntilHasPassed() {
        AppUser user = user();
        user.setLockedUntil(Instant.now().minusSeconds(60));
        assertThat(service.isLocked(user)).isFalse();
    }

    @Test
    void recordFailedAttempt_incrementsCount_withoutLocking_belowThreshold() {
        AppUser user = user();
        user.setFailedLoginCount(2);
        when(userRepository.save(user)).thenReturn(user);

        service.recordFailedAttempt(user);

        assertThat(user.getFailedLoginCount()).isEqualTo(3);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void recordFailedAttempt_locks_onFifthFailure() {
        AppUser user = user();
        user.setFailedLoginCount(4);
        when(userRepository.save(user)).thenReturn(user);

        service.recordFailedAttempt(user);

        assertThat(user.getFailedLoginCount()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isAfter(Instant.now());
        org.mockito.Mockito.verify(securityEventLogger).record(
                org.mockito.ArgumentMatchers.eq(SecurityEventType.ACCOUNT_LOCKED), any(), any(), any(), any());
    }

    @Test
    void recordSuccessfulLogin_resetsCountAndLock() {
        AppUser user = user();
        user.setFailedLoginCount(3);
        user.setLockedUntil(Instant.now().minusSeconds(60));
        when(userRepository.save(user)).thenReturn(user);

        service.recordSuccessfulLogin(user);

        assertThat(user.getFailedLoginCount()).isEqualTo(0);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void unlock_resetsAndLogsEvent() {
        AppUser user = user();
        user.setFailedLoginCount(5);
        user.setLockedUntil(Instant.now().plusSeconds(600));
        UUID actorId = UUID.randomUUID();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        AppUser result = service.unlock(user.getId(), actorId);

        assertThat(result.getFailedLoginCount()).isEqualTo(0);
        assertThat(result.getLockedUntil()).isNull();
        org.mockito.Mockito.verify(securityEventLogger).record(SecurityEventType.ACCOUNT_UNLOCKED, actorId, "tester", null,
                "Unlocked by administrator");
    }
}
