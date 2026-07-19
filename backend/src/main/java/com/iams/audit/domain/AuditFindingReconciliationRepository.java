package com.iams.audit.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditFindingReconciliationRepository extends JpaRepository<AuditFindingReconciliation, UUID> {

    Optional<AuditFindingReconciliation> findByFindingId(UUID findingId);

    /**
     * US-AUD-18: how many of an audit's findings were reconciled through the formal
     * US-AUD-21 workflow. This is the ONLY path that counts toward the missing-rate
     * reduction in cross-cycle analytics (AC-AUD-18: "that reconciliation - and only
     * that path - counts"); an asset that merely reappears without a reconciliation
     * record is deliberately not credited.
     */
    @Query("SELECT COUNT(r) FROM AuditFindingReconciliation r WHERE r.finding.audit.id = :auditId")
    long countByAuditId(UUID auditId);
}
