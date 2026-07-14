package com.iams.usr.application;

import com.iams.common.exception.NotFoundException;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-SEC-09: lock an account after 5 consecutive failed logins, for a fixed
 * cool-down. Only a wrong *password* against a real account counts against
 * the counter - a correct password against a deactivated account doesn't
 * (that's a status problem, not a credential-guessing attempt, and counting
 * it would let an attacker learn an account is deactivated by watching the
 * lockout threshold).
 */
@Service
public class UserLockoutService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final AppUserRepository userRepository;
    private final SecurityEventLogger securityEventLogger;

    public UserLockoutService(AppUserRepository userRepository, SecurityEventLogger securityEventLogger) {
        this.userRepository = userRepository;
        this.securityEventLogger = securityEventLogger;
    }

    public boolean isLocked(AppUser user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
    }

    @Transactional
    public void recordFailedAttempt(AppUser user) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        if (user.getFailedLoginCount() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
            securityEventLogger.record(SecurityEventType.ACCOUNT_LOCKED, user.getId(), user.getUsername(), null,
                    "Locked after " + MAX_FAILED_ATTEMPTS + " consecutive failed login attempts");
        }
        userRepository.save(user);
    }

    @Transactional
    public void recordSuccessfulLogin(AppUser user) {
        if (user.getFailedLoginCount() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    /** US-SEC-09 AC: admin unlock. Self-service unlock isn't built - there's no email/notification system yet to deliver it through. */
    @Transactional
    public AppUser unlock(UUID userId, UUID actorId) {
        AppUser user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("AppUser", userId));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        AppUser saved = userRepository.save(user);
        securityEventLogger.record(SecurityEventType.ACCOUNT_UNLOCKED, actorId, user.getUsername(), null,
                "Unlocked by administrator");
        return saved;
    }
}
