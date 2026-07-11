package com.iams.asset.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetCustomFieldDefinitionRepository extends JpaRepository<AssetCustomFieldDefinition, UUID> {

    List<AssetCustomFieldDefinition> findByCategoryIdOrderByDisplayOrder(UUID categoryId);
}
