package com.iams.analytics.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {

    /** US-ANL-03: the whole adoption matrix in one grouped query - exact per-role distinct-user counts, no in-memory event scan. */
    @Query("SELECT new com.iams.analytics.domain.AdoptionAggregate(e.role, e.module, COUNT(e), COUNT(DISTINCT e.userId), MAX(e.occurredAt)) "
            + "FROM UsageEvent e WHERE e.occurredAt >= :since GROUP BY e.role, e.module")
    List<AdoptionAggregate> aggregateSince(Instant since);
}
