package com.iams.audit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditFindingRepository extends JpaRepository<AuditFinding, UUID> {

    @Query("SELECT f FROM AuditFinding f JOIN FETCH f.asset WHERE f.audit.id = :auditId")
    List<AuditFinding> findByAuditIdWithAsset(UUID auditId);

    @Query("SELECT f FROM AuditFinding f JOIN FETCH f.asset WHERE f.id = :id")
    Optional<AuditFinding> findByIdWithAsset(UUID id);

    Optional<AuditFinding> findByAuditIdAndAssetId(UUID auditId, UUID assetId);

    /**
     * US-AUD-16: exceptions are anything not a clean verified find - Missing,
     * Out of Scope, Scope Changed, or a Verified finding with a damage-level
     * condition. Both bind params are always non-null (caller passes
     * FindingStatus.VERIFIED and the fixed damage-condition list), so this
     * is a plain, unambiguous JPQL query - no PGJDBC parameter-type-inference
     * risk (that bug class only bites nullable IS-NULL-guarded params, see
     * AssetRepositoryImpl's Javadoc).
     */
    @Query("SELECT f FROM AuditFinding f JOIN FETCH f.asset WHERE f.audit.id = :auditId "
            + "AND (f.status <> :verifiedStatus OR f.condition IN :damageConditions)")
    List<AuditFinding> findExceptionsByAuditId(UUID auditId, FindingStatus verifiedStatus, List<AssetCondition> damageConditions);

    long countByAuditIdAndStatus(UUID auditId, FindingStatus status);

    long countByAuditId(UUID auditId);

    /** US-AUD-23 closure gate: any SCOPE_CHANGED finding still without a resolved disposition. */
    boolean existsByAuditIdAndStatusAndScopeChangeDispositionIsNull(UUID auditId, FindingStatus status);

    /**
     * A system-classified Missing finding (verifiedByUserId null - see
     * AuditFinding's Javadoc) never came from a real scan, so it's the one
     * finding shape safe to delete rather than correct: AuditWorkflowService.reject()
     * removes these so a reopened audit's auditor can actually rescan that
     * asset. Without this, US-AUD-24's finding immutability would trap a
     * rejected-and-reopened audit forever - the asset could never be
     * verified because a finding row for it already exists.
     */
    List<AuditFinding> findByAuditIdAndStatusAndVerifiedByUserIdIsNull(UUID auditId, FindingStatus status);
}
