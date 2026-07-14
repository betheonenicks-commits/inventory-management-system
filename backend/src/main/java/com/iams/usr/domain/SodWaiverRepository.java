package com.iams.usr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SodWaiverRepository extends JpaRepository<SodWaiver, UUID> {

    /**
     * signedOffBy is FetchType.LAZY and open-in-view is disabled - same reasoning as
     * every other JOIN FETCH variant in this codebase (see AppUserRepository).
     */
    @Query("SELECT w FROM SodWaiver w JOIN FETCH w.signedOffBy ORDER BY w.createdAt DESC")
    List<SodWaiver> findAllWithSignedOffByOrderByCreatedAtDesc();

    @Query("SELECT w FROM SodWaiver w JOIN FETCH w.signedOffBy WHERE w.id = :id")
    Optional<SodWaiver> findByIdWithSignedOffBy(UUID id);

    /** AC-USR-09-H: is there an active waiver for this scope, for the reroute path (US-AUD-22) to check once it exists. */
    boolean existsByScopeAndActiveTrue(String scope);

    /** US-AUD-22: once a waiver is known to be active for a scope, this resolves who the reroute's "waiver-designated alternate" actually is - the officer who signed off. */
    @Query("SELECT w FROM SodWaiver w JOIN FETCH w.signedOffBy WHERE w.scope = :scope AND w.active = true ORDER BY w.createdAt DESC")
    List<SodWaiver> findActiveByScopeOrderByCreatedAtDesc(String scope);
}
