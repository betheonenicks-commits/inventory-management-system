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

    /**
     * US-USR-04 / AUD-03 scope: does this audit have at least one expected asset whose
     * location is within the given org-scope path prefix? Used to decide whether a
     * scoped caller may see a no-org-node (category/asset-list-scoped) audit - its
     * footprint is the set of its expected-asset locations. OrgNode.path is a
     * materialized ancestor-id chain of UUIDs and '/' with no LIKE metacharacters, so
     * {@code LIKE prefix || '%'} is the exact DB equivalent of {@code path.startsWith(prefix)}.
     */
    @Query("SELECT (COUNT(e) > 0) FROM AuditExpectedAsset e "
            + "WHERE e.audit.id = :auditId AND e.asset.orgNode.path LIKE CONCAT(:pathPrefix, '%')")
    boolean existsInScope(UUID auditId, String pathPrefix);

    /** US-USR-04 / AUD-03 scope, batched for list endpoints: which of these audits have an expected asset within the scope prefix. */
    @Query("SELECT DISTINCT e.audit.id FROM AuditExpectedAsset e "
            + "WHERE e.audit.id IN :auditIds AND e.asset.orgNode.path LIKE CONCAT(:pathPrefix, '%')")
    List<UUID> findAuditIdsInScope(java.util.Collection<UUID> auditIds, String pathPrefix);

    /** US-AUD-23: every audit in the given status that expects this asset - the automatic mid-audit scope-change trigger's lookup. */
    @Query("SELECT e FROM AuditExpectedAsset e JOIN FETCH e.audit WHERE e.asset.id = :assetId AND e.audit.status = :status")
    List<AuditExpectedAsset> findByAssetIdAndAuditStatus(UUID assetId, AuditStatus status);
}
