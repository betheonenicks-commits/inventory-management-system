package com.iams.compliance.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, UUID> {

    Optional<RetentionPolicy> findByEntityType(RetentionEntityType entityType);
}
