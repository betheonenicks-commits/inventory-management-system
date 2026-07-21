package com.iams.usr.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.security.InvalidUnlockTokenException;
import com.iams.notification.application.NotificationDispatchService;
import com.iams.notification.domain.NotificationEventType;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.domain.AccountUnlockToken;
import com.iams.usr.domain.AccountUnlockTokenRepository;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
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
    private static final Duration UNLOCK_TOKEN_TTL = Duration.ofMinutes(30);
    private static final int TOKEN_BYTES = 32;

    private final AppUserRepository userRepository;
    private final SecurityEventLogger securityEventLogger;
    private final AccountUnlockTokenRepository unlockTokenRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final SecureRandom random = new SecureRandom();

    public UserLockoutService(AppUserRepository userRepository, SecurityEventLogger securityEventLogger,
                               AccountUnlockTokenRepository unlockTokenRepository,
                               NotificationDispatchService notificationDispatchService) {
        this.userRepository = userRepository;
        this.securityEventLogger = securityEventLogger;
        this.unlockTokenRepository = unlockTokenRepository;
        this.notificationDispatchService = notificationDispatchService;
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

    /** US-SEC-09 AC: admin unlock. */
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

    /**
     * US-SEC-09's self-service half. Deliberately silent on both "unknown
     * username" and "not currently locked" - same no-enumeration-leak
     * reasoning AuthController's login already applies (AC-SEC-04-X): a
     * caller probing usernames must see identical behavior either way.
     * Only an actually-locked account gets a token and an email at all.
     */
    @Transactional
    public void requestSelfServiceUnlock(String username) {
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !isLocked(user)) {
            return;
        }

        String rawToken = generateRawToken();
        AccountUnlockToken token = new AccountUnlockToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(UNLOCK_TOKEN_TTL));
        unlockTokenRepository.save(token);

        Map<String, String> vars = Map.of(
                "summary", "Account unlock requested",
                "detail", "Your IAMS account is locked. Use this unlock code within "
                        + UNLOCK_TOKEN_TTL.toMinutes() + " minutes to regain access: " + rawToken
                        + ". If you didn't request this, no action is needed.");
        notificationDispatchService.dispatch(NotificationEventType.SECURITY_ALERT, Set.of(user.getId()), vars, null);

        securityEventLogger.record(SecurityEventType.ACCOUNT_UNLOCK_REQUESTED, user.getId(), username, null,
                "Self-service unlock email requested");
    }

    /** US-SEC-09's self-service half: redeem the emailed code, one time, before it expires. */
    @Transactional
    public void confirmSelfServiceUnlock(String rawToken) {
        AccountUnlockToken token = unlockTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidUnlockTokenException::new);
        if (token.isUsed() || token.isExpired()) {
            throw new InvalidUnlockTokenException();
        }
        AppUser user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidUnlockTokenException::new);

        token.setUsedAt(Instant.now());
        unlockTokenRepository.save(token);

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        securityEventLogger.record(SecurityEventType.ACCOUNT_UNLOCKED, user.getId(), user.getUsername(), null,
                "Self-service unlock via emailed code");
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
