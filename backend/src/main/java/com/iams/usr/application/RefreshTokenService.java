package com.iams.usr.application;

import com.iams.common.security.DevSecurityProperties;
import com.iams.common.security.InvalidRefreshTokenException;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.domain.RefreshToken;
import com.iams.usr.domain.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-SEC-01 (revocable refresh tokens) and US-SEC-06 (idle timeout, enforced
 * at exchange time - see rotate()'s idle check). Rotation-based: every
 * successful rotate() revokes the presented token and issues a brand new
 * one, so a stolen-then-replayed token becomes detectable (already revoked)
 * rather than silently valid until its natural expiry - see
 * REFRESH_TOKEN_REUSE_DETECTED.
 * <p>
 * Only the SHA-256 hash of the raw token is ever persisted, matching how
 * passwords are never stored raw either. The raw value exists only in the
 * response body and the caller's memory.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final SecurityEventLogger securityEventLogger;
    private final long expirationMinutes;
    private final long idleTimeoutMinutes;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, SecurityEventLogger securityEventLogger,
                                DevSecurityProperties properties) {
        this.repository = repository;
        this.securityEventLogger = securityEventLogger;
        this.expirationMinutes = properties.getJwt().getRefreshExpirationMinutes();
        this.idleTimeoutMinutes = properties.getJwt().getRefreshIdleTimeoutMinutes();
    }

    public record Rotated(UUID userId, String rawToken) {
    }

    @Transactional
    public String issue(UUID userId) {
        String rawToken = generateRawToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES));
        repository.save(token);
        return rawToken;
    }

    /** US-SEC-01-H (exchange) + US-SEC-06 (idle timeout). Refuses and logs on any invalid/expired/revoked/idle-too-long presentation. */
    @Transactional
    public Rotated rotate(String rawToken) {
        RefreshToken token = repository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);

        if (token.isRevoked()) {
            // AC-SEC-01-X: an already-revoked token presented again - could be a legitimate
            // double-submit, but is exactly what token theft/replay looks like, so it's
            // logged distinctly rather than folded into a generic failure.
            securityEventLogger.record(SecurityEventType.REFRESH_TOKEN_REUSE_DETECTED, token.getUserId(), null, null,
                    "Already-revoked refresh token presented again");
            throw new InvalidRefreshTokenException();
        }
        if (token.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        Instant lastActivity = token.getLastUsedAt() != null ? token.getLastUsedAt() : token.getIssuedAt();
        if (lastActivity.isBefore(Instant.now().minus(idleTimeoutMinutes, ChronoUnit.MINUTES))) {
            token.setRevokedAt(Instant.now());
            repository.save(token);
            securityEventLogger.record(SecurityEventType.SESSION_EXPIRED, token.getUserId(), null, null,
                    "Refresh token idle for longer than the configured timeout");
            throw new InvalidRefreshTokenException();
        }

        token.setRevokedAt(Instant.now());
        token.setLastUsedAt(Instant.now());
        repository.save(token);

        String newRawToken = generateRawToken();
        RefreshToken next = new RefreshToken();
        next.setUserId(token.getUserId());
        next.setTokenHash(hash(newRawToken));
        next.setExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES));
        next.setLastUsedAt(Instant.now());
        repository.save(next);

        return new Rotated(token.getUserId(), newRawToken);
    }

    /** US-SEC-01: logout - revoke exactly the presented token. */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            repository.save(token);
        });
    }

    /** US-USR-08 / US-SEC-01: logout-all, and called on account deactivation so offboarding actually ends live sessions. */
    @Transactional
    public void revokeAll(UUID userId) {
        List<RefreshToken> live = repository.findByUserIdAndRevokedAtIsNull(userId);
        Instant now = Instant.now();
        for (RefreshToken token : live) {
            token.setRevokedAt(now);
        }
        repository.saveAll(live);
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
