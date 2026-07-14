package com.iams.lifecycle.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssetTransferRequestRepository extends JpaRepository<AssetTransferRequest, UUID> {

    @Query("SELECT t FROM AssetTransferRequest t JOIN FETCH t.asset LEFT JOIN FETCH t.fromOrgNode JOIN FETCH t.toOrgNode WHERE t.id = :id")
    Optional<AssetTransferRequest> findByIdWithAssociations(UUID id);

    @Query("SELECT t FROM AssetTransferRequest t JOIN FETCH t.asset LEFT JOIN FETCH t.fromOrgNode JOIN FETCH t.toOrgNode ORDER BY t.requestedAt DESC")
    List<AssetTransferRequest> findAllWithAssociationsOrderByRequestedAtDesc();

    @Query("SELECT t FROM AssetTransferRequest t JOIN FETCH t.asset LEFT JOIN FETCH t.fromOrgNode JOIN FETCH t.toOrgNode "
            + "WHERE t.asset.id = :assetId ORDER BY t.requestedAt DESC")
    List<AssetTransferRequest> findByAssetIdWithAssociationsOrderByRequestedAtDesc(UUID assetId);

    @Query("SELECT t FROM AssetTransferRequest t JOIN FETCH t.asset LEFT JOIN FETCH t.fromOrgNode JOIN FETCH t.toOrgNode "
            + "WHERE t.status = :status ORDER BY t.requestedAt DESC")
    List<AssetTransferRequest> findByStatusWithAssociationsOrderByRequestedAtDesc(LifecycleRequestStatus status);
}
