package com.iams.usr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.common.security.DevSecurityProperties;
import com.iams.common.security.InvalidRefreshTokenException;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.domain.RefreshToken;
import com.iams.usr.domain.RefreshTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repository;
    @Mock private SecurityEventLogger securityEventLogger;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        DevSecurityProperties properties = new DevSecurityProperties();
        properties.getJwt().setRefreshExpirationMinutes(43200);
        properties.getJwt().setRefreshIdleTimeoutMinutes(1440);
        service = new RefreshTokenService(repository, securityEventLogger, properties);
    }

    private RefreshToken liveToken(UUID userId, Instant lastUsedAt) {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setTokenHash("irrelevant-for-these-tests");
        token.setIssuedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        token.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        token.setLastUsedAt(lastUsedAt);
        return token;
    }

    @Test
    void issue_savesANewTokenAndReturnsARawValue() {
        UUID userId = UUID.randomUUID();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        String raw = service.issue(userId);

        assertThat(raw).isNotBlank();
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(raw); // never stores the raw value
    }

    @Test
    void rotate_rejectsUnknownToken() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("nonexistent"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotate_rejectsAlreadyRevokedToken_andLogsReuse() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = liveToken(userId, Instant.now());
        token.setRevokedAt(Instant.now().minusSeconds(60));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.rotate("stolen-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(securityEventLogger).record(SecurityEventType.REFRESH_TOKEN_REUSE_DETECTED, userId, null, null,
                "Already-revoked refresh token presented again");
    }

    @Test
    void rotate_rejectsExpiredToken() {
        RefreshToken token = liveToken(UUID.randomUUID(), Instant.now());
        token.setExpiresAt(Instant.now().minusSeconds(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.rotate("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotate_rejectsIdleTooLong_andRevokesIt_andLogsSessionExpired() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = liveToken(userId, Instant.now().minus(2, ChronoUnit.DAYS)); // idle timeout is 1440min = 24h
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.rotate("idle-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(token.isRevoked()).isTrue();
        verify(securityEventLogger).record(SecurityEventType.SESSION_EXPIRED, userId, null, null,
                "Refresh token idle for longer than the configured timeout");
    }

    @Test
    void rotate_succeeds_revokesOldAndIssuesNew() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = liveToken(userId, Instant.now().minusSeconds(60));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.Rotated result = service.rotate("valid-token");

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.rawToken()).isNotBlank();
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void revoke_marksTheMatchingTokenRevoked() {
        RefreshToken token = liveToken(UUID.randomUUID(), Instant.now());
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.revoke("some-token");

        assertThat(token.isRevoked()).isTrue();
        verify(repository).save(token);
    }

    @Test
    void revoke_isANoOp_whenTokenNotFound() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        service.revoke("nonexistent");
        // no exception - success, and nothing to save
    }

    @Test
    void revokeAll_revokesEveryLiveTokenForTheUser() {
        UUID userId = UUID.randomUUID();
        RefreshToken t1 = liveToken(userId, Instant.now());
        RefreshToken t2 = liveToken(userId, Instant.now());
        when(repository.findByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(t1, t2));

        service.revokeAll(userId);

        assertThat(t1.isRevoked()).isTrue();
        assertThat(t2.isRevoked()).isTrue();
        verify(repository).saveAll(List.of(t1, t2));
    }
}
