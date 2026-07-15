package com.iams.audit.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditRepository extends JpaRepository<Audit, UUID> {

    /**
     * scopeOrgNode/scopeCategory are FetchType.LAZY and open-in-view is
     * disabled - same LazyInitializationException reasoning as
     * AssetRepository.findByIdWithAssociations - every caller here maps to a
     * response DTO after the transactional service method returns.
     */
    @Query("SELECT a FROM Audit a LEFT JOIN FETCH a.scopeOrgNode LEFT JOIN FETCH a.scopeCategory WHERE a.id = :id")
    Optional<Audit> findByIdWithAssociations(UUID id);

    @Query("SELECT a FROM Audit a LEFT JOIN FETCH a.scopeOrgNode LEFT JOIN FETCH a.scopeCategory ORDER BY a.createdAt DESC")
    List<Audit> findAllWithAssociationsOrderByCreatedAtDesc();

    @Query("SELECT a FROM Audit a LEFT JOIN FETCH a.scopeOrgNode LEFT JOIN FETCH a.scopeCategory "
            + "WHERE a.status = :status ORDER BY a.createdAt DESC")
    List<Audit> findByStatusWithAssociationsOrderByCreatedAtDesc(AuditStatus status);

    /** US-DSH-05: the audit-calendar window - scope filtering happens in the caller, same as list(). */
    @Query("SELECT a FROM Audit a LEFT JOIN FETCH a.scopeOrgNode "
            + "WHERE a.scheduledDate BETWEEN :from AND :to ORDER BY a.scheduledDate ASC")
    List<Audit> findScheduledBetween(LocalDate from, LocalDate to);
}
