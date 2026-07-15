package com.iams.audit.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditFindingReconciliationRepository extends JpaRepository<AuditFindingReconciliation, UUID> {

    Optional<AuditFindingReconciliation> findByFindingId(UUID findingId);
}
