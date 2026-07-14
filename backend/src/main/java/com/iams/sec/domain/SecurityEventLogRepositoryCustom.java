package com.iams.sec.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * US-SEC-11: search/filter, implemented with the JPA Criteria API in
 * SecurityEventLogRepositoryImpl - the same pattern AssetRepositoryImpl uses.
 * A plain JPQL "(:param IS NULL OR ...)" query was tried first but PGJDBC
 * cannot infer a bind parameter's type when its only appearance is on both
 * sides of an IS-NULL-guarded OR with a nullable Instant (found via live
 * click-testing: "could not determine data type of parameter"). The Criteria
 * API sidesteps this entirely - a null filter just isn't added as a
 * predicate, so PGJDBC never sees an ambiguous placeholder.
 */
public interface SecurityEventLogRepositoryCustom {

    Page<SecurityEventLog> search(UUID actorUserId, SecurityEventType eventType, Instant from, Instant to, Pageable pageable);
}
