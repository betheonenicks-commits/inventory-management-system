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

    /** US-AUD-23: every audit in the given status that expects this asset - the automatic mid-audit scope-change trigger's lookup. */
    @Query("SELECT e FROM AuditExpectedAsset e JOIN FETCH e.audit WHERE e.asset.id = :assetId AND e.audit.status = :status")
    List<AuditExpectedAsset> findByAssetIdAndAuditStatus(UUID assetId, AuditStatus status);
}
