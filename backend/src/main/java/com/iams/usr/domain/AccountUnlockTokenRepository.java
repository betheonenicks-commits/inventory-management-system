package com.iams.usr.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountUnlockTokenRepository extends JpaRepository<AccountUnlockToken, UUID> {

    Optional<AccountUnlockToken> findByTokenHash(String tokenHash);
}
