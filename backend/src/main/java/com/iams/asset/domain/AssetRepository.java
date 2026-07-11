package com.iams.asset.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID>, AssetRepositoryCustom {

    boolean existsByCategoryId(UUID categoryId);
}
