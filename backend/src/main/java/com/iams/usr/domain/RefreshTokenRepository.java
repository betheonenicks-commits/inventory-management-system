package com.iams.usr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** US-USR-08/logout-all: every still-live token for a user, to revoke in one pass. */
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
}
