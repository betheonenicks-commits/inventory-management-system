package com.iams.asset.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetHistoryEventRepository extends JpaRepository<AssetHistoryEvent, UUID> {

    Page<AssetHistoryEvent> findByAssetIdOrderByCreatedAtDesc(UUID assetId, Pageable pageable);

    long countByAssetId(UUID assetId);
}
