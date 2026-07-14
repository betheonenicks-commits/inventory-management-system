package com.iams.lifecycle.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, UUID> {

    List<ApprovalDelegation> findByDelegatorUserIdOrderByValidFromDesc(UUID delegatorUserId);

    /** US-LIF-15: the delegate currently in effect for this delegator, if any - both endpoints of the window are inclusive. */
    @Query("SELECT d FROM ApprovalDelegation d WHERE d.delegatorUserId = :delegatorUserId AND d.active = true "
            + "AND d.validFrom <= :asOf AND d.validTo >= :asOf ORDER BY d.validFrom DESC")
    Optional<ApprovalDelegation> findActiveDelegation(UUID delegatorUserId, Instant asOf);
}
