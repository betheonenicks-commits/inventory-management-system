package com.iams.sec.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SecurityEventLogRepository extends JpaRepository<SecurityEventLog, UUID>, SecurityEventLogRepositoryCustom {

    /**
     * US-CMP-01: the actual retention-purge deletion, returns the row count removed. Annotated
     * @Transactional so it is self-sufficient - RetentionPolicyService.purge() is deliberately
     * non-transactional (so a per-person anonymize failure can't poison the whole run), so this
     * derived delete can no longer rely on an ambient service-level transaction.
     */
    @Transactional
    long deleteByCreatedAtBefore(Instant cutoff);
}
