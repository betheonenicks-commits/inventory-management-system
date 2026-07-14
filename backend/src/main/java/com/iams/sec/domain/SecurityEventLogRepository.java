package com.iams.sec.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityEventLogRepository extends JpaRepository<SecurityEventLog, UUID>, SecurityEventLogRepositoryCustom {

    /** US-CMP-01: the actual retention-purge deletion, returns the row count removed. */
    long deleteByCreatedAtBefore(Instant cutoff);
}
