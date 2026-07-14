package com.iams.lifecycle.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssetDisposalRequestRepository extends JpaRepository<AssetDisposalRequest, UUID> {

    @Query("SELECT d FROM AssetDisposalRequest d JOIN FETCH d.asset WHERE d.id = :id")
    Optional<AssetDisposalRequest> findByIdWithAsset(UUID id);

    @Query("SELECT d FROM AssetDisposalRequest d JOIN FETCH d.asset ORDER BY d.requestedAt DESC")
    List<AssetDisposalRequest> findAllWithAssetOrderByRequestedAtDesc();

    @Query("SELECT d FROM AssetDisposalRequest d JOIN FETCH d.asset WHERE d.asset.id = :assetId ORDER BY d.requestedAt DESC")
    List<AssetDisposalRequest> findByAssetIdWithAssetOrderByRequestedAtDesc(UUID assetId);

    @Query("SELECT d FROM AssetDisposalRequest d JOIN FETCH d.asset WHERE d.status = :status ORDER BY d.requestedAt DESC")
    List<AssetDisposalRequest> findByStatusWithAssetOrderByRequestedAtDesc(LifecycleRequestStatus status);
}
