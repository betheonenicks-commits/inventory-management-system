package com.iams.compliance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalHoldRepository extends JpaRepository<LegalHold, UUID> {

    Optional<LegalHold> findByScopeTypeAndScopeIdAndActiveTrue(LegalHoldScopeType scopeType, UUID scopeId);

    List<LegalHold> findByScopeTypeOrderByCreatedAtDesc(LegalHoldScopeType scopeType);

    List<LegalHold> findAllByOrderByCreatedAtDesc();
}
