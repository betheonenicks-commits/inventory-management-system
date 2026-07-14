package com.iams.asset.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDetailRepository extends JpaRepository<VehicleDetail, UUID> {

    Optional<VehicleDetail> findByAssetId(UUID assetId);
}
