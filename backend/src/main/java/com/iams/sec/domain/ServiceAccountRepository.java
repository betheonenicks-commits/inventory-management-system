package com.iams.sec.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, UUID> {

    Optional<ServiceAccount> findByApiKeyHash(String apiKeyHash);

    boolean existsByName(String name);

    List<ServiceAccount> findAllByOrderByCreatedAtDesc();
}
