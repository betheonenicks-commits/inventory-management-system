package com.iams.audit.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditExpectedAssetRepository extends JpaRepository<AuditExpectedAsset, UUID> {

    @Query("SELECT e FROM AuditExpectedAsset e JOIN FETCH e.asset WHERE e.audit.id = :auditId")
    List<AuditExpectedAsset> findByAuditIdWithAsset(UUID auditId);

    boolean existsByAuditIdAndAssetId(UUID auditId, UUID assetId);

    long countByAuditId(UUID auditId);

    /**
     * US-AUD-03: expected-asset count per org node (location) for one audit - the
     * "sub-scope" breakdown's expected column. Grouped in the database via a
     * constructor expression; asset.orgNode is a non-null association, so the
     * implicit inner join drops nothing and these counts sum back to
     * {@link #countByAuditId} exactly.
     */
    @Query("SELECT new com.iams.audit.domain.AuditSubScopeCount("
            + "e.asset.orgNode.id, e.asset.orgNode.name, e.asset.orgNode.code, count(e)) "
            + "FROM AuditExpectedAsset e WHERE e.audit.id = :auditId "
            + "GROUP BY e.asset.orgNode.id, e.asset.orgNode.name, e.asset.orgNode.code")
    List<AuditSubScopeCount> countExpectedByOrgNode(UUID auditId);

    /** US-AUD-23: every audit in the given status that expects this asset - the automatic mid-audit scope-change trigger's lookup. */
    @Query("SELECT e FROM AuditExpectedAsset e JOIN FETCH e.audit WHERE e.asset.id = :assetId AND e.audit.status = :status")
    List<AuditExpectedAsset> findByAssetIdAndAuditStatus(UUID assetId, AuditStatus status);
}
