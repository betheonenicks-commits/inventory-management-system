package com.iams.usr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-SEC-01: a revocable refresh token. Rotation-based - RefreshTokenService
 * revokes the presented row and issues a new one on every successful
 * exchange, rather than reusing the same row, so a stolen-then-replayed
 * token is detectable as "already revoked" instead of silently valid until
 * its natural expiry. userId is a plain UUID (not a JPA relation) - nothing
 * here needs to navigate to AppUser's other fields, only compare/carry the id.
 */
@Getter
@Setter
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
    }
}
