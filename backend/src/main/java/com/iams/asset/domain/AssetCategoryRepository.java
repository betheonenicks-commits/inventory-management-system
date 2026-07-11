package com.iams.asset.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, UUID> {

    boolean existsByCode(String code);

    boolean existsByName(String name);

    Optional<AssetCategory> findByCode(String code);
}
