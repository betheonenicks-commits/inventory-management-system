package com.iams.asset.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetDepreciationOverrideRepository extends JpaRepository<AssetDepreciationOverride, UUID> {

    Optional<AssetDepreciationOverride> findByAssetId(UUID assetId);
}
