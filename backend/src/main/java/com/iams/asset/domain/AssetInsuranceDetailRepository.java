package com.iams.asset.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetInsuranceDetailRepository extends JpaRepository<AssetInsuranceDetail, UUID> {

    Optional<AssetInsuranceDetail> findByAssetId(UUID assetId);
}
