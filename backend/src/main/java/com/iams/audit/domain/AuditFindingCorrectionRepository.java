package com.iams.audit.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditFindingCorrectionRepository extends JpaRepository<AuditFindingCorrection, UUID> {

    List<AuditFindingCorrection> findByFindingIdOrderByCreatedAtAsc(UUID findingId);

    /** Batch form, for mapping a whole audit's findings without N+1 queries. */
    List<AuditFindingCorrection> findByFindingIdInOrderByCreatedAtAsc(Collection<UUID> findingIds);
}
