package com.iams.maintenance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepairEventRepository extends JpaRepository<RepairEvent, UUID> {

    @Query("SELECT r FROM RepairEvent r JOIN FETCH r.asset WHERE r.id = :id")
    Optional<RepairEvent> findByIdWithAsset(UUID id);

    @Query("SELECT r FROM RepairEvent r JOIN FETCH r.asset WHERE r.asset.id = :assetId ORDER BY r.createdAt DESC")
    List<RepairEvent> findByAssetIdWithAssetOrderByCreatedAtDesc(UUID assetId);

    @Query("SELECT r FROM RepairEvent r JOIN FETCH r.asset ORDER BY r.createdAt DESC")
    List<RepairEvent> findAllWithAssetOrderByCreatedAtDesc();
}
