package com.iams.usr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.iams.common.security.InvalidUnlockTokenException;
import com.iams.notification.application.NotificationDispatchService;
import com.iams.notification.domain.NotificationEventType;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.domain.AccountUnlockToken;
import com.iams.usr.domain.AccountUnlockTokenRepository;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserLockoutServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private SecurityEventLogger securityEventLogger;
    @Mock private AccountUnlockTokenRepository unlockTokenRepository;
    @Mock private NotificationDispatchService notificationDispatchService;

    private UserLockoutService service;

    @BeforeEach
    void setUp() {
        service = new UserLockoutService(userRepository, securityEventLogger, unlockTokenRepository,
                notificationDispatchService);
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

    @Test
    void requestSelfServiceUnlock_noOp_whenUsernameUnknown() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        service.requestSelfServiceUnlock("ghost");

        verifyNoInteractions(notificationDispatchService, unlockTokenRepository);
        verify(securityEventLogger, never()).record(eq(SecurityEventType.ACCOUNT_UNLOCK_REQUESTED), any(), any(), any(), any());
    }

    @Test
    void requestSelfServiceUnlock_noOp_whenNotLocked() {
        AppUser user = user();
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));

        service.requestSelfServiceUnlock("tester");

        verifyNoInteractions(notificationDispatchService, unlockTokenRepository);
    }

    @Test
    void requestSelfServiceUnlock_issuesTokenAndDispatchesEmail_whenLocked() {
        AppUser user = user();
        user.setLockedUntil(Instant.now().plusSeconds(600));
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));

        service.requestSelfServiceUnlock("tester");

        ArgumentCaptor<AccountUnlockToken> tokenCaptor = ArgumentCaptor.forClass(AccountUnlockToken.class);
        verify(unlockTokenRepository).save(tokenCaptor.capture());
        AccountUnlockToken saved = tokenCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());

        verify(notificationDispatchService).dispatch(eq(NotificationEventType.SECURITY_ALERT),
                eq(Set.of(user.getId())), anyMap(), isNull());
        verify(securityEventLogger).record(eq(SecurityEventType.ACCOUNT_UNLOCK_REQUESTED), eq(user.getId()),
                eq("tester"), isNull(), anyString());
    }

    @Test
    void confirmSelfServiceUnlock_rejectsUnknownToken() {
        when(unlockTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmSelfServiceUnlock("bogus"))
                .isInstanceOf(InvalidUnlockTokenException.class);
    }

    @Test
    void confirmSelfServiceUnlock_rejectsExpiredToken() {
        AccountUnlockToken token = new AccountUnlockToken();
        token.setUserId(UUID.randomUUID());
        token.setExpiresAt(Instant.now().minusSeconds(60));
        when(unlockTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirmSelfServiceUnlock("expired"))
                .isInstanceOf(InvalidUnlockTokenException.class);
    }

    @Test
    void confirmSelfServiceUnlock_rejectsAlreadyUsedToken() {
        AccountUnlockToken token = new AccountUnlockToken();
        token.setUserId(UUID.randomUUID());
        token.setExpiresAt(Instant.now().plusSeconds(600));
        token.setUsedAt(Instant.now().minusSeconds(30));
        when(unlockTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirmSelfServiceUnlock("reused"))
                .isInstanceOf(InvalidUnlockTokenException.class);
    }

    @Test
    void confirmSelfServiceUnlock_unlocksUser_marksTokenUsed_andLogsEvent() {
        AppUser user = user();
        user.setFailedLoginCount(5);
        user.setLockedUntil(Instant.now().plusSeconds(600));
        AccountUnlockToken token = new AccountUnlockToken();
        token.setUserId(user.getId());
        token.setExpiresAt(Instant.now().plusSeconds(600));
        when(unlockTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        service.confirmSelfServiceUnlock("valid-raw-token");

        assertThat(user.getFailedLoginCount()).isEqualTo(0);
        assertThat(user.getLockedUntil()).isNull();
        assertThat(token.getUsedAt()).isNotNull();
        verify(unlockTokenRepository).save(token);
        verify(userRepository).save(user);
        verify(securityEventLogger).record(SecurityEventType.ACCOUNT_UNLOCKED, user.getId(), "tester", null,
                "Self-service unlock via emailed code");
    }
}
